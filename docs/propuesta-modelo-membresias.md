# Propuesta: Nuevo Modelo de Membresías

## Problema del modelo actual

- Un solo registro de membresía por estudiante que se modifica con cada pago.
- Las fechas se recalculan y "deslizan" con cada pago, causando bugs de aritmética de calendario.
- El pago anticipado mueve `fechaInicio`/`fechaFin` al futuro en el mismo registro.
- No hay trazabilidad natural: se necesita una tabla `MembresiaHistorial` separada.
- `observacionPago` y `fechaLimiteCompromiso` viven en `Estudiante` aunque son datos de un período específico.
- Dos métodos distintos (`activarMembresiaEstudiante` y `actualizarMembresiaAlPagar`) hacen lo mismo para Wompi y efectivo respectivamente.

---

## Modelo propuesto

### Principio base

> **Un pago = una membresía.** Cada vez que un estudiante paga, se crea un registro nuevo de membresía para ese período. El registro no se modifica una vez creado, solo cambia de estado.

---

## Entidad `Membresia` — cambios

### Campos nuevos

| Campo | Tipo | Descripción |
|---|---|---|
| `tipoMembresia` | Enum | Origen del pago |
| `estadoMembresia` | Enum | Estado del ciclo de vida |
| `observacion` | String | Nota del período (ej: acuerdo, problema de tarjeta) |
| `fechaLimiteCompromiso` | LocalDate | Solo para ACUERDO_PAGO: fecha límite para pagar |

### Campos que se eliminan o migran

| Campo | Acción |
|---|---|
| `Boolean estado` | Reemplazado por `estadoMembresia` |
| `motivoCambio` | Se mantiene para trazabilidad interna |

### `tipoMembresia` enum

```
ONLINE       — pago confirmado vía Wompi
EFECTIVO     — pago en efectivo registrado por admin
ACUERDO_PAGO — acuerdo gestionado manualmente por admin
```

### `estadoMembresia` enum

```
PENDIENTE_PAGO — creada, esperando confirmación de pago (acuerdos o Wompi pendiente)
PAGADA         — pago confirmado, período vigente o futuro
FINALIZADA     — fechaFin ya pasó, ciclo cerrado
CANCELADA      — cancelada (acuerdo vencido, admin, error)
```

---

## Entidad `Estudiante` — cambios

### Campo nuevo

| Campo | Tipo | Descripción |
|---|---|---|
| `diaPago` | Integer | Día fijo del mes de vencimiento (billing anchor) |

### Campos que se eliminan

| Campo | Razón |
|---|---|
| `observacionPago` | Se mueve a `Membresia.observacion` |
| `fechaLimiteCompromiso` | Se mueve a `Membresia.fechaLimiteCompromiso` |

### `estadoPago` del estudiante — se mantiene

```
PENDIENTE      — sin membresía pagada aún
AL_DIA         — tiene membresía PAGADA vigente
EN_MORA        — membresía FINALIZADA sin siguiente período pagado
COMPROMISO_PAGO — tiene acuerdo de pago activo (membresía PENDIENTE_PAGO tipo ACUERDO_PAGO)
SIN_MEMBRESIA  — estudiante inactivo, sin membresía
```

---

## Regla de membresía activa

Una membresía se considera **activa** cuando:

```
estadoMembresia = PAGADA
AND fechaFin >= hoy
AND fechaInicio <= hoy (o es la próxima en activarse)
```

Un estudiante puede tener:
- Una membresía **activa** (período en curso)
- Una o más membresías **futuras** PAGADAS (pagos anticipados)
- Membresías **FINALIZADAS** (historial)
- Una membresía **PENDIENTE_PAGO** tipo ACUERDO_PAGO (acuerdo vigente)

---

## `diaPago` — Billing anchor

Almacena el día fijo de vencimiento del estudiante. Funciona igual que el billing anchor de Netflix/Stripe.

**Reglas:**
- Se setea al crear la primera membresía: `diaPago = fechaInicio.getDayOfMonth()`
- Todos los cálculos de `fechaFin` usan `diaPago`:

```java
YearMonth target = YearMonth.from(fechaInicio).plusMonths(meses);
int dia = Math.min(estudiante.getDiaPago(), target.lengthOfMonth());
LocalDate fechaFin = target.atDay(dia);
```

- Ejemplo: `diaPago=31`, febrero → usa 28; marzo → usa 31 ✓
- Cuando el admin cambia la fecha fin manualmente al 15 → `estudiante.diaPago = 15` y de ahí en adelante todos los períodos vencen el 15.

---

## Flujos por tipo de pago

### Pago Wompi (ONLINE)

```
Webhook APPROVED
  → buscar Pago PENDIENTE con esa referencia
  → actualizar Pago a PAGADO
  → crear Membresía:
      tipo = ONLINE
      estadoMembresia = PAGADA
      fechaInicio = diaPago del mes actual (o siguiente si ya pasó)
      fechaFin = fechaInicio + meses según monto (usando diaPago)
      pagoOrigen = pago confirmado
      ajustadoManualmente = false
  → estudiante.estadoPago = AL_DIA
```

### Pago en efectivo (EFECTIVO)

```
POST /api/estudiantes/{id}/pago-efectivo
  → crear Pago (EFECTIVO, PAGADO)
  → crear Membresía:
      tipo = EFECTIVO
      estadoMembresia = PAGADA
      fechaInicio / fechaFin según diaPago y monto
      pagoOrigen = pago registrado
  → estudiante.estadoPago = AL_DIA
```

### Acuerdo de pago (ACUERDO_PAGO)

```
Admin cambia estadoPago → COMPROMISO_PAGO
  → modificar membresía activa actual:
      tipo = ACUERDO_PAGO
      estadoMembresia = PENDIENTE_PAGO
      observacion = "acordó pagar el 20 de mayo"
      fechaLimiteCompromiso = 2026-05-20
      pagoOrigen = null (aún sin pago)
  → estudiante.estadoPago = COMPROMISO_PAGO

Cuando el estudiante finalmente paga:
  Opción A — Pago en efectivo (admin):
    → admin registra pago
    → crear Pago (EFECTIVO, PAGADO)
    → admin confirma membresía ACUERDO_PAGO:
        estadoMembresia = PAGADA
        pagoOrigen = pago registrado

  Opción B — Pago online (Wompi automático):
    → webhook APPROVED detecta pago del estudiante
    → crear Pago (ONLINE, PAGADO)
    → sistema busca membresía ACUERDO_PAGO PENDIENTE_PAGO del estudiante
    → estadoMembresia = PAGADA
    → pagoOrigen = pago Wompi

  En ambos casos:
    → observacion se conserva como historial
    → estudiante.estadoPago = AL_DIA

Si vence fechaLimiteCompromiso sin pago (lambda):
  → membresía: estadoMembresia = CANCELADA
  → estudiante.estadoPago = EN_MORA
```

### Pago anticipado

```
Estudiante paga en mayo para junio
  → membresía de mayo sigue PAGADA y activa
  → se crea nueva Membresía:
      estadoMembresia = PAGADA
      fechaInicio = primer día del período siguiente (usando diaPago)
      fechaFin = fechaInicio + meses
      pagoOrigen = nuevo pago
  → estudiante.estadoPago se mantiene AL_DIA
```

---

## Lambda medianoche — nueva lógica

```
Para cada estudiante activo:

1. Buscar membresía activa (PAGADA, fechaFin >= hoy)
   → Si existe: no hacer nada (está al día)

2. Si no hay membresía activa:
   a. Buscar membresía PAGADA con fechaInicio en el mes actual o próxima a iniciar
      → Si existe: activarla (ya es el período que empieza)
                   marcar la anterior como FINALIZADA
                   estudiante.estadoPago = AL_DIA

   b. Si no hay siguiente membresía pagada:
      → membresía vencida: estadoMembresia = FINALIZADA
      → estudiante.estadoPago = EN_MORA
      → iniciar flujo de recordatorios (ya existe)

3. Estudiantes EN_MORA > 15 días (feature flag):
   → membresía: estadoMembresia = CANCELADA
   → estudiante.estado = false (inactivo)
   → estudiante.estadoPago = SIN_MEMBRESIA

4. Estudiantes con ACUERDO_PAGO y fechaLimiteCompromiso vencida:
   → membresía: estadoMembresia = CANCELADA
   → estudiante.estadoPago = EN_MORA
```

---

## Unificación de métodos de pago

Hoy existen dos métodos distintos:
- `activarMembresiaEstudiante()` — usado por Wompi
- `actualizarMembresiaAlPagar()` — usado por efectivo

En el nuevo modelo se unifican en un solo método:

```java
crearMembresiaParaPago(Estudiante estudiante, Pago pago, TipoMembresia tipo)
```

Recibe el pago confirmado, calcula `fechaInicio`/`fechaFin` usando `estudiante.diaPago`, crea el registro de membresía y actualiza el `estadoPago` del estudiante.

---

## `ajustadoManualmente` — se mantiene en `Membresia`

Cuando el admin corrige manualmente la `fechaFin` de una membresía:
- `ajustadoManualmente = true`
- `estudiante.diaPago` se actualiza al nuevo día
- El próximo pago usa esa `fechaFin` como base del siguiente período
- Después del primer pago posterior, `ajustadoManualmente = false`

---

## Impacto en tablas existentes

| Tabla | Cambio |
|---|---|
| `membresia` | Agregar `tipo_membresia`, `estado_membresia`, `observacion`, `fecha_limite_compromiso`. Eliminar `estado` boolean |
| `estudiante` | Agregar `dia_pago`. Eliminar `observacion_pago`, `fecha_limite_compromiso` |
| `membresia_historial` | Puede simplificarse o eliminarse — el historial es la lista de membresías |
| `pago` | Sin cambios |

---

## Migración de datos existentes

```
Para cada membresía existente:
  - Si estado=true  AND fechaFin >= hoy  → estadoMembresia = PAGADA
  - Si estado=true  AND fechaFin <  hoy  → estadoMembresia = FINALIZADA
  - Si estado=false AND fechaFin != null → estadoMembresia = FINALIZADA
  - Si estado=false AND fechaFin = null  → estadoMembresia = CANCELADA

Para cada estudiante:
  - diaPago = fechaInicio de su membresía más reciente (getDayOfMonth())
  - observacionPago → mover a la membresía activa o más reciente
  - fechaLimiteCompromiso → mover a la membresía con tipo ACUERDO_PAGO si aplica
```

---

## Resumen de ventajas

1. **Sin bugs de cálculo de fechas** — cada membresía se crea una vez con fechas exactas
2. **Pago anticipado limpio** — registro independiente, no desplaza el período actual
3. **Acuerdos de pago como ciudadanos de primera clase** — tipo propio, trazabilidad completa
4. **Billing anchor (`diaPago`)** — el día de vencimiento nunca cambia salvo decisión explícita
5. **Historial inherente** — la lista de membresías ES el historial, sin tabla extra
6. **Un solo método de activación** — sin importar si el pago es Wompi o efectivo
7. **`observacion` y `fechaLimiteCompromiso` en el lugar correcto** — en la membresía del período, no en el estudiante
