# Sistema de Gestión de Estados de Pago de Estudiantes

## Descripción General

Este sistema permite gestionar el estado de pago de los estudiantes de la escuela de voleibol. El estado de pago se actualiza automáticamente cuando se realizan pagos online (Wompi) y manualmente para pagos en efectivo.

## Estados de Pago Disponibles

| Estado | Descripción | Color |
|--------|-------------|-------|
| `PENDIENTE` | Estado inicial al registrar un estudiante. No ha realizado ningún pago. | 🟠 Naranja |
| `AL_DIA` | Pago realizado dentro del período correspondiente. | 🟢 Verde |
| `EN_MORA` | No realizó el pago antes de la fecha límite del mes. | 🔴 Rojo |
| `COMPROMISO_PAGO` | Acuerdo de pago posterior, establecido manualmente. | 🔵 Azul |

## Flujo de Estados

```
                    ┌─────────────┐
                    │  REGISTRO   │
                    │ ESTUDIANTE  │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  PENDIENTE  │◄─────────────────┐
                    └──────┬──────┘                  │
                           │                         │
            ┌──────────────┼──────────────┐          │
            │              │              │          │
            ▼              ▼              ▼          │
     ┌────────────┐ ┌────────────┐ ┌────────────┐    │
     │ Pago Online│ │Pago Efectivo│ │  Vence el  │    │
     │  (Wompi)   │ │  (Manual)  │ │    Mes     │    │
     └─────┬──────┘ └─────┬──────┘ └─────┬──────┘    │
           │              │              │           │
           ▼              ▼              ▼           │
     ┌─────────────────────────┐  ┌─────────────┐    │
     │        AL_DIA           │  │   EN_MORA   │────┤
     └─────────────────────────┘  └──────┬──────┘    │
                                         │           │
                                         ▼           │
                                  ┌─────────────┐    │
                                  │ COMPROMISO  │    │
                                  │   DE PAGO   │────┘
                                  └─────────────┘
```

## API Endpoints

### Estudiantes con Estado de Pago

#### Listar todos los estudiantes con estado de pago
```http
GET /api/estudiantes/con-estado-pago
```
**Respuesta:** Lista de `EstudianteConEstadoPagoDTO`

#### Listar estudiantes activos con estado de pago
```http
GET /api/estudiantes/activos/con-estado-pago
```

#### Filtrar estudiantes por estado de pago
```http
GET /api/estudiantes/estado-pago/{estadoPago}
```
**Parámetros:** `PENDIENTE`, `AL_DIA`, `EN_MORA`, `COMPROMISO_PAGO`

#### Obtener estudiantes en mora
```http
GET /api/estudiantes/en-mora
```

#### Obtener estudiantes pendientes
```http
GET /api/estudiantes/pendientes-pago
```

#### Obtener estudiantes al día
```http
GET /api/estudiantes/al-dia
```

#### Obtener estudiantes por sede con estado de pago
```http
GET /api/estudiantes/sede/{idSede}/con-estado-pago
```

### Cambiar Estado de Pago

#### Cambiar estado de pago manualmente
```http
PATCH /api/estudiantes/{id}/estado-pago
```
**Body:**
```json
{
    "nuevoEstado": "AL_DIA",
    "observacion": "Pago verificado en caja",
    "mesPagado": "FEBRERO_2026"
}
```
**Estados válidos:** `PENDIENTE`, `AL_DIA`, `EN_MORA`, `COMPROMISO_PAGO`

### Registrar Pago en Efectivo

#### Registrar pago en efectivo
```http
POST /api/estudiantes/{id}/pago-efectivo
```
**Body:**
```json
{
    "mesPagado": "FEBRERO_2026",
    "valor": 50000,
    "observacion": "Pago en efectivo",
    "referenciaPago": "REC-001"
}
```
**Resultado:** Registra el pago y cambia automáticamente el estado a `AL_DIA`

### Verificación Manual de Estados

#### Ejecutar verificación de estados
```http
POST /api/estudiantes/verificar-estados-pago
```
Ejecuta manualmente la verificación de estados de pago de todos los estudiantes.

## Asistencia de Estudiantes con Estado de Pago

### Listar asistencias con estado de pago
```http
GET /api/asistencia-estudiante/con-estado-pago
```

### Por fecha
```http
GET /api/asistencia-estudiante/fecha/{fecha}/con-estado-pago
```
**Formato fecha:** `2026-02-02` (ISO DATE)

### Por equipo
```http
GET /api/asistencia-estudiante/equipo/{idEquipo}/con-estado-pago
```

### Por equipo y fecha
```http
GET /api/asistencia-estudiante/equipo/{idEquipo}/fecha/{fecha}/con-estado-pago
```

### Por estudiante
```http
GET /api/asistencia-estudiante/estudiante/{idEstudiante}/con-estado-pago
```

## DTOs Principales

### EstudianteConEstadoPagoDTO
```json
{
    "idEstudiante": 1,
    "nombreCompleto": "Juan Pérez",
    "numeroDocumento": "1234567890",
    "tipoDocumento": "CC",
    "edad": 15,
    "fotoUrl": "/uploads/estudiantes/foto.jpg",
    "celularEstudiante": "3001234567",
    "correoEstudiante": "juan@email.com",
    "estado": true,
    "estadoPago": "AL_DIA",
    "estadoPagoDescripcion": "Al día",
    "colorEstadoPago": "#66BB6A",
    "idSede": 1,
    "nombreSede": "Sede Principal",
    "nivelActual": "INTERMEDIO",
    "fechaUltimoPago": "2026-01-15",
    "mesUltimoPago": "ENERO_2026"
}
```

### AsistenciaEstudianteConEstadoPagoDTO
```json
{
    "idAsistencia": 1,
    "fecha": "2026-02-02",
    "asistio": true,
    "observaciones": "",
    "idEstudiante": 1,
    "nombreCompleto": "Juan Pérez",
    "numeroDocumento": "1234567890",
    "fotoUrl": "/uploads/estudiantes/foto.jpg",
    "edad": 15,
    "estadoPago": "AL_DIA",
    "estadoPagoDescripcion": "Al día",
    "colorEstadoPago": "#66BB6A",
    "idEquipo": 1,
    "nombreEquipo": "Sub-15 Masculino"
}
```

## Actualización Automática de Estados

### Pagos Online (Wompi)
Cuando se recibe un webhook de Wompi con estado `APPROVED`, el sistema:
1. Marca el pago como `PAGADO`
2. Actualiza el estado del estudiante a `AL_DIA`
3. Actualiza la membresía del estudiante

### Verificación Programada (Scheduler)
El sistema ejecuta verificaciones automáticas:
- **Diaria:** Todos los días a las 6:00 AM
- **Mensual:** El primer día de cada mes a las 00:01

Durante la verificación:
1. Se revisa si cada estudiante activo tiene pago del mes actual
2. Si no tiene pago y ya pasó su día de pago, se marca como `EN_MORA`

## Notas Importantes

1. **Estado inicial:** Todo estudiante nuevo comienza con estado `PENDIENTE`
2. **Pago online:** Actualiza automáticamente a `AL_DIA`
3. **Pago efectivo:** Requiere registro manual que actualiza a `AL_DIA`
4. **Mora automática:** Se detecta cuando pasa el día de pago configurado para el estudiante
5. **Compromiso de pago:** Solo se puede establecer manualmente

## Archivos Modificados

- `Estudiante.java` - Agregado enum `EstadoPago` y campo `estadoPago`
- `EstudianteRepository.java` - Nuevos métodos de búsqueda por estado de pago
- `EstudianteService.java` - Métodos para gestionar estados de pago
- `EstudianteController.java` - Nuevos endpoints de estado de pago
- `AsistenciaEstudianteService.java` - Métodos con estado de pago
- `AsistenciaEstudianteController.java` - Endpoints con estado de pago
- `AsistenciaEstudianteRepository.java` - Métodos de búsqueda adicionales
- `WompiService.java` - Actualización automática al aprobar pago
- `PagoRepository.java` - Métodos adicionales de búsqueda
- `GalacticosApplication.java` - Habilitado scheduling

## Archivos Creados

- `CambioEstadoPagoDTO.java` - DTO para cambio manual de estado
- `EstudianteConEstadoPagoDTO.java` - DTO con información de estado de pago
- `AsistenciaEstudianteConEstadoPagoDTO.java` - DTO de asistencia con estado de pago
- `RegistroPagoEfectivoDTO.java` - DTO para registro de pago en efectivo
- `EstadoPagoSchedulerService.java` - Servicio de verificación automática
