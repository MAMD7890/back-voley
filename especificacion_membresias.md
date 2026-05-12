# Especificación Técnica — Nuevo Modelo de Membresías

---

## 1. Principio Base

Un pago = una membresía. Cada vez que un estudiante paga, se crea un registro nuevo de membresía para ese período. El registro no se modifica una vez creado, salvo en los casos específicos de conversión de estado (mora → pagada, pendiente → pagada). Las fechas de inicio nunca se mueven después de la creación del registro.

---

## 2. Cambios en Entidades

### 2.1 Entidad Membresia — campos nuevos

| Campo | Tipo | Descripción |
|---|---|---|
| `tipoMembresia` | Enum | Origen del pago (ver enum sección 3) |
| `estadoMembresia` | Enum | Estado del ciclo de vida. Reemplaza Boolean estado |
| `observacion` | String | Nota del período. Ej: acuerdo negociado, problema de tarjeta |
| `fechaLimiteCompromiso` | LocalDate | Solo para ACUERDO_PAGO: fecha límite para pagar |
| `fechaLimiteGracia` | LocalDate | Para MORA y PENDIENTE_REGISTRO: fecha límite de los 15 días |
| `origenAcuerdo` | Enum | Solo para ACUERDO_PAGO: DESDE_PENDIENTE o DESDE_MORA |

### 2.2 Entidad Membresia — campos eliminados

| Campo | Acción |
|---|---|
| `Boolean estado` | Eliminado — reemplazado por estadoMembresia |
| `observacionPago` (estaba en Estudiante) | Movido a Membresia.observacion |
| `fechaLimiteCompromiso` (estaba en Estudiante) | Movido a Membresia.fechaLimiteCompromiso |

### 2.3 Entidad Estudiante — cambios

| Campo | Acción | Descripción |
|---|---|---|
| `diaPago` | NUEVO | Día fijo del mes de vencimiento (billing anchor). Se setea al crear la primera membresía |
| `observacionPago` | ELIMINAR | Se mueve a Membresia.observacion |
| `fechaLimiteCompromiso` | ELIMINAR | Se mueve a Membresia.fechaLimiteCompromiso |

El campo `estadoPago` del estudiante se mantiene sin cambios en su definición actual.

---

## 3. Enums

### 3.1 tipoMembresia

| Valor | Descripción |
|---|---|
| `ONLINE` | Pago confirmado vía Wompi |
| `EFECTIVO` | Pago en efectivo registrado por admin |
| `ACUERDO_PAGO` | Acuerdo gestionado manualmente por admin |
| `MORA` | Período en mora creado automáticamente al vencer sin pago siguiente |
| `PENDIENTE_REGISTRO` | Estudiante cargado por Excel, aún sin ningún pago realizado |

### 3.2 estadoMembresia

| Valor | Descripción |
|---|---|
| `PENDIENTE_PAGO` | Creada, esperando confirmación de pago. Aplica a ACUERDO_PAGO y PENDIENTE_REGISTRO |
| `EN_MORA` | Período vencido sin pago siguiente. Solo aplica a tipo MORA |
| `PAGADA` | Pago confirmado, período vigente o futuro |
| `FINALIZADA` | fechaFin ya pasó y había un período siguiente pagado. Ciclo cerrado correctamente |
| `CANCELADA` | Cancelada por vencimiento de gracia, vencimiento de acuerdo o por admin |

### 3.3 estadoPago del Estudiante (sin cambios, referencia)

| Valor | Cuándo aplica |
|---|---|
| `PENDIENTE` | Sin membresía pagada aún (recién registrado por Excel) |
| `AL_DIA` | Tiene membresía PAGADA vigente |
| `EN_MORA` | Membresía FINALIZADA sin siguiente período pagado |
| `COMPROMISO_PAGO` | Tiene acuerdo de pago activo (membresía PENDIENTE_PAGO tipo ACUERDO_PAGO) |
| `SIN_MEMBRESIA` | Estudiante inactivo, sin membresía activa |

---

## 4. Billing Anchor (diaPago)

Almacena el día fijo de vencimiento del estudiante. Funciona igual que el billing anchor de Stripe/Netflix.

**Reglas:**
- Se asigna al crear la primera membresía: `diaPago = fechaInicio.getDayOfMonth()`
- Todos los cálculos de fechaFin usan diaPago como ancla
- Si el mes destino tiene menos días que diaPago, se usa el último día del mes. Ej: diaPago=31 en febrero → 28
- Cuando el admin corrige manualmente la fechaFin de una membresía, `estudiante.diaPago` se actualiza al nuevo día
- Cuando un estudiante SIN_MEMBRESIA paga, diaPago se recalcula desde la nueva fechaInicio

**Algoritmo de cálculo de fechaFin:** tomar YearMonth de fechaInicio, sumar los meses que cubre el pago, usar `Math.min(diaPago, diasDelMes)` como día del mes resultante.

---

## 5. Regla de fechaInicio por Contexto

> Si existe un período abierto sin pago, siempre se conserva su fechaInicio. Si no hay nada, la fechaInicio es la fecha del pago.

| Situación del estudiante | fechaInicio de la nueva membresía |
|---|---|
| Tiene membresía EN_MORA (tipo MORA) | fechaInicio de la membresía de mora — se conserva |
| Tiene membresía PENDIENTE_PAGO (tipo PENDIENTE_REGISTRO) | fechaInicio del registro — se conserva |
| Tiene membresía PENDIENTE_PAGO (tipo ACUERDO_PAGO) | fechaInicio del acuerdo — se conserva |
| SIN_MEMBRESIA (inactivo que vuelve a pagar) | Fecha del pago — nueva |
| Primer pago normal | Fecha del pago — nueva |

---

## 6. Regla de Autenticación

> **REGLA ABSOLUTA: El login NUNCA se bloquea por estado de membresía ni por estado del estudiante (activo/inactivo). Todo estudiante con credenciales válidas puede autenticarse siempre.**

Esta regla existe para que los estudiantes inactivos puedan entrar al sistema a realizar su pago y reactivar su cuenta sin necesidad de contactar al admin. Esta restricción no va en el login sino en cada endpoint según el estadoPago.

**Control de acceso post-login:**

| estadoPago | Login | Contenido / Clases | Pantalla de pago | Banner aviso |
|---|---|---|---|---|
| `AL_DIA` | ✓ Siempre | ✓ Acceso completo | Opcional | No |
| `EN_MORA` | ✓ Siempre | ✓ Acceso completo | ✓ Visible | ✓ Días restantes |
| `COMPROMISO_PAGO` | ✓ Siempre | ✓ Acceso completo | ✓ Visible | ✓ Fecha límite acuerdo |
| `PENDIENTE` | ✓ Siempre | ✗ Sin acceso | ✓ Único destino | ✓ Mensaje bienvenida |
| `SIN_MEMBRESIA` | ✓ Siempre | ✗ Sin acceso | ✓ Único destino | ✓ Reactiva tu cuenta |

Cuando un estudiante inactivo (`estado=false`) realiza el pago exitosamente, el sistema lo activa automáticamente (`estado=true`) en el mismo flujo.

---

## 7. Flujos por Tipo de Pago

### 7.1 Pago Online — ONLINE (Wompi)
- Webhook APPROVED llega al sistema
- Se busca el Pago PENDIENTE con esa referencia y se marca PAGADO
- Se llama `crearMembresiaParaPago()` con tipo ONLINE
- El método detecta si hay membresía convertible (ver sección 8)
- Si hay convertible: se convierte conservando fechaInicio
- Si no hay convertible: fechaInicio = fecha del pago
- `estudiante.estadoPago = AL_DIA`. Si estaba inactivo, se activa

### 7.2 Pago en Efectivo — EFECTIVO
- Admin registra el pago desde el panel
- Se crea el registro Pago (EFECTIVO, PAGADO)
- Se llama `crearMembresiaParaPago()` con tipo EFECTIVO
- Misma lógica de detección de convertible que en ONLINE
- `estudiante.estadoPago = AL_DIA`. Si estaba inactivo, se activa

### 7.3 Acuerdo de Pago — ACUERDO_PAGO

**Creación del acuerdo:**
- Admin crea el acuerdo desde el panel
- El sistema busca la membresía reemplazable del estudiante: `tipo IN [PENDIENTE_REGISTRO, MORA]` AND `estadoMembresia IN [PENDIENTE_PAGO, EN_MORA]` AND `pagoOrigen = null`
- La membresía reemplazable pasa a CANCELADA
- Se crea membresía nueva: tipo=ACUERDO_PAGO, estadoMembresia=PENDIENTE_PAGO
- `fechaInicio` y `fechaFin` se copian del registro reemplazado — nunca se recalculan
- Se asigna `fechaLimiteCompromiso` según lo negociado por el admin
- `origenAcuerdo = DESDE_PENDIENTE` si el origen era PENDIENTE_REGISTRO, `DESDE_MORA` si era MORA
- `estudiante.estadoPago = COMPROMISO_PAGO`

**Resolución — el estudiante paga:**
- Llega pago (Wompi o efectivo)
- `crearMembresiaParaPago()` detecta la membresía ACUERDO_PAGO como convertible
- Se convierte: estadoMembresia=PAGADA, tipo=ONLINE o EFECTIVO, fechaFin recalculada según meses del pago, pagoOrigen asignado, fechaLimiteCompromiso y fechaLimiteGracia se limpian a null
- `estudiante.estadoPago = AL_DIA`

### 7.4 Carga por Excel — PENDIENTE_REGISTRO
- Al importar cada fila válida se crea el Estudiante con `estado=true`, `estadoPago=PENDIENTE`
- Se crea membresía: tipo=PENDIENTE_REGISTRO, estadoMembresia=PENDIENTE_PAGO
- `fechaInicio` = fecha de la importación
- `fechaFin` = fechaInicio + 1 mes usando diaPago
- `fechaLimiteGracia` = fechaInicio + 15 días
- `pagoOrigen = null`
- `estudiante.diaPago = fechaInicio.getDayOfMonth()`

### 7.5 Pago Anticipado
- El estudiante ya tiene una membresía PAGADA vigente y paga el siguiente período
- No hay membresía convertible (la vigente está PAGADA con pagoOrigen)
- Se crea membresía nueva con `fechaInicio` = día siguiente al fechaFin de la membresía vigente
- `estadoMembresia = PAGADA` desde el inicio
- `estudiante.estadoPago` se mantiene AL_DIA

---

## 8. Método Unificado crearMembresiaParaPago()

Reemplaza los anteriores `activarMembresiaEstudiante()` y `actualizarMembresiaAlPagar()`. Recibe el estudiante, el pago confirmado y el tipo (ONLINE o EFECTIVO).

**Paso 1 — Buscar membresía convertible:**
- Condición: `tipo IN [MORA, PENDIENTE_REGISTRO, ACUERDO_PAGO]` AND `estadoMembresia IN [EN_MORA, PENDIENTE_PAGO]` AND `pagoOrigen IS NULL`

**Paso 2a — Si hay convertible, actualizar ese registro:**
- Calcular meses según monto del pago
- Recalcular fechaFin desde fechaInicio del registro existente usando diaPago
- Asignar estadoMembresia = PAGADA
- Asignar tipo = ONLINE o EFECTIVO
- Asignar pagoOrigen = pago
- Limpiar fechaLimiteGracia y fechaLimiteCompromiso a null

**Paso 2b — Si NO hay convertible, crear membresía nueva:**
- fechaInicio = fecha del pago
- Calcular fechaFin usando diaPago
- Crear registro con estadoMembresia = PAGADA
- Actualizar `estudiante.diaPago = fechaInicio.getDayOfMonth()`

**Paso 3 — Siempre al final:**
- Si `estudiante.estado == false` → `estudiante.estado = true` (reactivar)
- `estudiante.estadoPago = AL_DIA`

---

## 9. Jobs Nocturnos

Se ejecutan a medianoche. Cada job tiene una sola responsabilidad. Si uno falla, los demás siguen ejecutándose de forma independiente.

### Job 1 — ActivarPeriodosFuturos
- Busca membresías PAGADA con `fechaInicio = hoy`
- Para cada una: busca la membresía anterior del mismo estudiante (la que cubría hasta ayer) y la marca FINALIZADA
- `estudiante.estadoPago = AL_DIA`

### Job 2 — DetectarVencimientos
- Busca membresías PAGADA con `fechaFin < hoy`
- Verifica que el estudiante NO tenga otra membresía PAGADA o EN_MORA vigente
- Si no tiene: la membresía vencida pasa a FINALIZADA
- Se crea membresía nueva tipo MORA, estadoMembresia=EN_MORA:
  - `fechaInicio` = fechaFin de la membresía vencida + 1 día
  - `fechaFin` = fechaInicio + 1 mes usando diaPago
  - `fechaLimiteGracia` = fechaInicio + 15 días
  - `pagoOrigen = null`
- `estudiante.estadoPago = EN_MORA`

### Job 3 — CancelarGraciasVencidas
- Busca: `estadoMembresia IN [EN_MORA, PENDIENTE_PAGO]` AND `tipo IN [MORA, PENDIENTE_REGISTRO]` AND `fechaLimiteGracia < hoy` AND `pagoOrigen IS NULL`
- Para cada una: `estadoMembresia = CANCELADA`
- `estudiante.estado = false`
- `estudiante.estadoPago = SIN_MEMBRESIA`

> El ACUERDO_PAGO vencido no pasa por aquí sino por Job 4, que tiene lógica distinta.

### Job 4 — CancelarAcuerdosVencidos
- Busca: `tipo = ACUERDO_PAGO` AND `fechaLimiteCompromiso < hoy` AND `pagoOrigen IS NULL`
- Para cada una: `estadoMembresia = CANCELADA`

**Si `origenAcuerdo = DESDE_PENDIENTE`** (nunca tuvo acceso activo):
- `estudiante.estado = false`
- `estudiante.estadoPago = SIN_MEMBRESIA`
- No se crea membresía de mora — nunca tuvo acceso que proteger

**Si `origenAcuerdo = DESDE_MORA`** (sí tuvo acceso activo antes):
- `estudiante.estadoPago = EN_MORA`
- Se crea membresía nueva tipo MORA con `fechaLimiteGracia = hoy + 15 días`
- El estudiante sigue activo — Job 3 lo cancelará si no paga en 15 días

---

## 10. Job de Notificaciones

El job existente se extiende con cuatro ramas. Cada rama usa un mensaje distinto porque el contexto es diferente.

| Rama | Condición | Días de disparo | Mensaje |
|---|---|---|---|
| Membresía próxima a vencer | `estadoMembresia=PAGADA` | 7, 5, 3, 1 días antes de fechaFin | "Tu membresía vence en X días" |
| Mora activa | `estadoMembresia=EN_MORA`, tipo=MORA, pagoOrigen=null | 5, 3, 1 días antes de fechaLimiteGracia | "Tienes X días para pagar o perderás el acceso" |
| Registro pendiente | `estadoMembresia=PENDIENTE_PAGO`, tipo=PENDIENTE_REGISTRO, pagoOrigen=null | Día 1 (bienvenida) y 5, 3, 1 antes de fechaLimiteGracia | "Bienvenido, tienes X días para activar tu membresía" |
| Acuerdo de pago | `estadoMembresia=PENDIENTE_PAGO`, tipo=ACUERDO_PAGO, pagoOrigen=null | 5, 3, 1 días antes de fechaLimiteCompromiso | "Tienes X días para cumplir tu acuerdo de pago" |

---

## 11. Ciclo de Vida Completo por Tipo

| Tipo | Nace como | Si paga antes del límite | Si NO paga — acción |
|---|---|---|---|
| `ONLINE` / `EFECTIVO` | PAGADA | — (ya está pagada) | Al vencer sin siguiente: FINALIZADA + se crea MORA |
| `MORA` | EN_MORA | → PAGADA, conserva fechaInicio de mora | fechaLimiteGracia vence: CANCELADA, estudiante inactivo |
| `PENDIENTE_REGISTRO` | PENDIENTE_PAGO | → PAGADA, conserva fechaInicio de carga | fechaLimiteGracia vence: CANCELADA, estudiante inactivo |
| `ACUERDO` desde PENDIENTE | PENDIENTE_PAGO | → PAGADA, conserva fechaInicio del acuerdo | fechaLimiteCompromiso vence: CANCELADA, estudiante inactivo directo |
| `ACUERDO` desde MORA | PENDIENTE_PAGO | → PAGADA, conserva fechaInicio del acuerdo | fechaLimiteCompromiso vence: CANCELADA, crea membresía MORA nueva 15d |

---

## 12. Migración de Datos Existentes

### 12.1 Tabla membresia

| Condición del registro existente | estadoMembresia resultante |
|---|---|
| `estado=true` AND `fechaFin >= hoy` | PAGADA |
| `estado=true` AND `fechaFin < hoy` | FINALIZADA |
| `estado=false` AND `fechaFin != null` | FINALIZADA |
| `estado=false` AND `fechaFin = null` | CANCELADA |

Para todos los registros migrados: `tipoMembresia = EFECTIVO` como valor por defecto. Los campos nuevos `fechaLimiteGracia`, `origenAcuerdo` y `fechaLimiteCompromiso` quedan en null.

### 12.2 Tabla estudiante

- `diaPago` = `getDayOfMonth()` de la fechaInicio de la membresía más reciente del estudiante
- Si no tiene membresías: `diaPago` = `getDayOfMonth()` de hoy
- `observacionPago` → copiar a `Membresia.observacion` de la membresía activa o más reciente, luego eliminar el campo
- `fechaLimiteCompromiso` → copiar a la membresía ACUERDO_PAGO activa si existe, luego eliminar el campo

### 12.3 Tabla membresia_historial

Puede simplificarse o eliminarse. La lista de membresías por estudiante ordenada por `fechaInicio DESC` es el historial completo. Evaluar si algún proceso actual depende de esa tabla antes de eliminarla.

---

## 13. Cambios en Base de Datos

| Tabla | Cambio |
|---|---|
| `membresia` | AGREGAR: `tipo_membresia`, `estado_membresia`, `observacion`, `fecha_limite_compromiso`, `fecha_limite_gracia`, `origen_acuerdo` (nullable) |
| `membresia` | ELIMINAR: `estado` (boolean) |
| `estudiante` | AGREGAR: `dia_pago` (integer) |
| `estudiante` | ELIMINAR: `observacion_pago`, `fecha_limite_compromiso` |
| `membresia_historial` | EVALUAR eliminación (ver sección 12.3) |
| `pago` | Sin cambios |
