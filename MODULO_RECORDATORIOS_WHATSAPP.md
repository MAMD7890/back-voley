# 📱 Módulo de Recordatorios de Pago por WhatsApp

## Descripción General

Este módulo implementa un sistema automatizado de notificaciones por WhatsApp para recordar a los estudiantes sobre el vencimiento de sus membresías. Utiliza la API oficial de Twilio para el envío de mensajes.

---

## 🏗️ Arquitectura del Módulo

```
┌─────────────────────────────────────────────────────────────────┐
│                    MÓDULO DE RECORDATORIOS                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐    ┌──────────────────────┐                │
│  │ TwilioConfig    │───▶│ TwilioWhatsAppService│                │
│  │ (Configuración) │    │ (Envío de mensajes)  │                │
│  └─────────────────┘    └──────────┬───────────┘                │
│                                     │                            │
│  ┌─────────────────┐    ┌──────────▼───────────┐                │
│  │ @Scheduled      │───▶│RecordatorioScheduler │                │
│  │ (8:00 AM diario)│    │      Service         │                │
│  └─────────────────┘    └──────────┬───────────┘                │
│                                     │                            │
│  ┌─────────────────┐    ┌──────────▼───────────┐                │
│  │RecordatorioPago │◀───│RecordatorioPago      │                │
│  │   Repository    │    │     Service          │                │
│  └────────┬────────┘    └──────────────────────┘                │
│           │                                                      │
│  ┌────────▼────────┐                                            │
│  │ RecordatorioPago│                                            │
│  │    (Entity)     │                                            │
│  └─────────────────┘                                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 Reglas de Negocio

### Momentos de Envío

| Momento | Días | Tipo de Recordatorio |
|---------|------|---------------------|
| 5 días antes | -5 | `CINCO_DIAS_ANTES` |
| 3 días antes | -3 | `TRES_DIAS_ANTES` |
| Día del vencimiento | 0 | `DIA_VENCIMIENTO` |
| 3 días después | +3 | `TRES_DIAS_DESPUES` |
| 5 días después | +5 | `CINCO_DIAS_DESPUES` |

### Condiciones para Envío

El sistema envía recordatorios SOLO si:
- ✅ La membresía tiene fecha de vencimiento correspondiente
- ✅ El estudiante está **activo** (`estado = true`)
- ✅ El estudiante tiene número de WhatsApp/celular válido
- ✅ NO se ha enviado previamente el mismo tipo de recordatorio para esa membresía

### Prioridad de Números de Contacto

Los recordatorios de pago se envían prioritariamente al **tutor** (responsable del pago):

1. `telefonoTutor` (principal - responsable del pago)
2. `whatsappEstudiante` (alternativa)
3. `celularEstudiante` (último recurso)

---

## 🔧 Configuración

### application.properties

```properties
# ========================
# TWILIO WHATSAPP CONFIGURATION
# ========================
twilio.account-sid=YOUR_TWILIO_ACCOUNT_SID
twilio.auth-token=YOUR_TWILIO_AUTH_TOKEN
twilio.whatsapp-from=whatsapp:+14155238886
twilio.enabled=true
twilio.sandbox=true

# ========================
# RECORDATORIOS CONFIGURATION
# ========================
recordatorio.enabled=true
recordatorio.max-reintentos=3
recordatorio.cron=0 0 8 * * *
```

### Variables de Configuración

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `twilio.account-sid` | SID de la cuenta Twilio | Requerido |
| `twilio.auth-token` | Token de autenticación | Requerido |
| `twilio.whatsapp-from` | Número de WhatsApp Twilio | `whatsapp:+14155238886` |
| `twilio.enabled` | Habilita/deshabilita Twilio | `true` |
| `twilio.sandbox` | Modo sandbox | `true` |
| `recordatorio.enabled` | Habilita recordatorios | `true` |
| `recordatorio.max-reintentos` | Máximo de reintentos | `3` |
| `recordatorio.cron` | Expresión cron | `0 0 8 * * *` (8:00 AM) |

---

## 🗂️ Archivos del Módulo

### Entidades
- `RecordatorioPago.java` - Entidad JPA con campos extendidos

### Enums
- `TipoRecordatorio` - Tipos de recordatorio (-5, -3, 0, +3, +5 días)
- `EstadoEnvio` - Estados: ENVIADO, FALLIDO, PENDIENTE

### Configuración
- `TwilioConfig.java` - Configuración e inicialización de Twilio

### Servicios
- `TwilioWhatsAppService.java` - Envío de mensajes WhatsApp
- `RecordatorioSchedulerService.java` - Tarea programada diaria
- `RecordatorioPagoService.java` - Operaciones CRUD (actualizado)

### Repositorios
- `RecordatorioPagoRepository.java` - Consultas JPA especializadas
- `MembresiaRepository.java` - Nuevas consultas (actualizado)

### DTOs
- `WhatsAppMessageResult.java` - Resultado de envío
- `RecordatorioPagoDto.java` - DTO para transferencia
- `RecordatorioEstadisticasDto.java` - Estadísticas

### Controladores
- `RecordatorioAdminController.java` - API REST de administración

---

## 📡 API REST

### Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/admin/recordatorios` | Lista todos los recordatorios |
| GET | `/api/admin/recordatorios/{id}` | Obtiene recordatorio por ID |
| GET | `/api/admin/recordatorios/estudiante/{id}` | Historial por estudiante |
| POST | `/api/admin/recordatorios/ejecutar` | Ejecuta envío manual |
| GET | `/api/admin/recordatorios/estadisticas` | Estadísticas del sistema |
| DELETE | `/api/admin/recordatorios/{id}` | Elimina un recordatorio |
| GET | `/api/admin/recordatorios/health` | Health check del servicio |

### Ejemplo de Respuesta - Estadísticas

```json
{
  "servicioTwilioActivo": true,
  "recordatoriosHabilitados": true,
  "maxReintentos": 3,
  "enviosHoy": {
    "ENVIADO": 15,
    "FALLIDO": 2
  },
  "pendientesReintento": 2,
  "totalRecordatorios": 150,
  "estadisticasPorEstado": {
    "ENVIADO": 140,
    "FALLIDO": 10
  }
}
```

---

## 🧪 Pruebas Locales

### 1. Configurar Twilio Sandbox

1. Crear cuenta en [Twilio](https://www.twilio.com/try-twilio)
2. Ir a **Messaging > Try it out > Send a WhatsApp message**
3. Escanear el código QR con WhatsApp o enviar el código al número indicado
4. Copiar las credenciales a `application.properties`

### 2. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

### 3. Probar Envío Manual

```bash
# Ejecutar recordatorios manualmente (requiere autenticación admin)
curl -X POST http://localhost:8080/api/admin/recordatorios/ejecutar \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Verificar Estadísticas

```bash
curl http://localhost:8080/api/admin/recordatorios/estadisticas \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Probar con Cron Modificado

Para pruebas rápidas, cambiar el cron a cada minuto:

```properties
recordatorio.cron=0 * * * * *
```

---

## 📝 Mensajes de WhatsApp

### Ejemplos de Mensajes Personalizados

**5 días antes del vencimiento:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
📅 *Recordatorio de Pago*

Hola *Juan Pérez* 👋

Te recordamos que tu membresía vence en *5 días* (el 08/02/2026).

💰 Realiza tu pago a tiempo para continuar disfrutando de:
   ✅ Entrenamientos regulares
   ✅ Acceso a todas las instalaciones
   ✅ Participación en torneos

📲 Puedes pagar en línea o en nuestras oficinas.

¡Gracias por ser parte de la familia Galácticos! 🌟
```

**3 días antes del vencimiento:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
⏰ *Recordatorio Importante*

Hola *Juan Pérez* 👋

Tu membresía vence en *3 días* (el 05/02/2026).

⚠️ No olvides renovar para seguir entrenando con nosotros.

💳 *Métodos de pago disponibles:*
   • Pago en línea (tarjeta/PSE)
   • Efectivo en recepción
   • Transferencia bancaria

¿Tienes dudas? Responde a este mensaje.

🏐 ¡Te esperamos en la cancha!
```

**Día del vencimiento:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
🚨 *¡ATENCIÓN! Vencimiento HOY*

Hola *Juan Pérez* 👋

⚠️ *Tu membresía vence HOY 03/02/2026*

Para continuar entrenando sin interrupciones, te invitamos a realizar tu pago lo antes posible.

💡 *Recuerda:* Si no renuevas hoy, mañana no podrás asistir a clases.

📞 ¿Necesitas ayuda? Contáctanos.

¡Gracias por entrenar con Galácticos! 🌟
```

**3 días después del vencimiento:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
🔔 *Membresía Vencida*

Hola *Juan Pérez* 👋

Tu membresía venció hace *3 días* (desde el 31/01/2026).

😔 Te extrañamos en los entrenamientos.

💪 *Renueva ahora y continúa mejorando:*
   • Tus habilidades técnicas
   • Tu condición física
   • Tu trabajo en equipo

📲 Realiza tu pago y vuelve a entrenar mañana mismo.

¿Tienes alguna dificultad? Escríbenos, podemos ayudarte. 🤝
```

**5 días después del vencimiento:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
🚨 *URGENTE - Membresía Vencida*

Hola *Juan Pérez* 👋

Tu membresía lleva *5 días vencida* (desde el 29/01/2026).

⚠️ *Tu lugar en el equipo está en riesgo.*

Sabemos que pueden surgir imprevistos. Si tienes alguna dificultad para pagar:

📞 *Comunícate con nosotros* y buscaremos una solución juntos:
   • Planes de pago flexibles
   • Opciones de financiamiento

💪 No dejes que esto detenga tu progreso.

¡Te esperamos de vuelta! 🏐
```

### Mensajes Adicionales

**Mensaje de Bienvenida (al registrar estudiante):**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
🎉 *¡Bienvenido/a a la Familia!*

Hola *Juan Pérez* 👋

¡Nos alegra mucho que te unas a nosotros!

📋 *Tu información:*
   🏆 Equipo: Sub-15 Masculino

📱 Por este medio recibirás:
   • Recordatorios de pago
   • Información de entrenamientos
   • Novedades del equipo

¿Tienes preguntas? ¡Estamos para ayudarte!

¡Nos vemos en la cancha! 🌟
```

**Confirmación de Pago:**
```
🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*
━━━━━━━━━━━━━━━━━━━━━
✅ *Pago Recibido*

Hola *Juan Pérez* 👋

¡Gracias por tu pago! Tu membresía está al día.

📋 *Detalles:*
   💰 Monto: $150.000
   📅 Período: Febrero 2026
   📆 Próximo vencimiento: 03/03/2026

¡Sigue entrenando y dando lo mejor! 💪

🏐 ¡Nos vemos en la cancha!
```

---

## 🔄 Flujo de Ejecución

```
┌─────────────────────────────────────────────────────────────┐
│                  EJECUCIÓN DIARIA (8:00 AM)                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. ¿Recordatorios habilitados?                             │
│     │                                                        │
│     ├─ NO ──▶ Terminar                                      │
│     │                                                        │
│     └─ SÍ ──▶ 2. Para cada día de recordatorio (-5,-3,0,+3,+5)│
│                  │                                           │
│                  └─▶ 3. Buscar membresías con fecha correspondiente│
│                       │                                      │
│                       └─▶ 4. Para cada membresía:           │
│                            │                                 │
│                            ├─ ¿Estudiante activo? ──NO──▶ Omitir│
│                            │                                 │
│                            ├─ ¿Tiene WhatsApp? ──NO──▶ Omitir│
│                            │                                 │
│                            ├─ ¿Ya se envió? ──SÍ──▶ Omitir  │
│                            │                                 │
│                            └─ Enviar mensaje por Twilio     │
│                                 │                            │
│                                 ├─ ÉXITO ──▶ Guardar (ENVIADO)│
│                                 │                            │
│                                 └─ ERROR ──▶ Guardar (FALLIDO)│
│                                                              │
│  5. Procesar reintentos de mensajes fallidos                │
│                                                              │
│  6. Generar log de estadísticas                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Consideraciones Importantes

### Límites de Twilio

- **Sandbox:** Solo puede enviar a números que hayan optado por recibir mensajes
- **Producción:** Requiere número de WhatsApp Business aprobado
- **Rate limits:** Consultar documentación de Twilio

### Prevención de Duplicados

El sistema previene duplicados mediante:
- Restricción única en BD: `(id_membresia, tipo_recordatorio, fecha_vencimiento_referencia)`
- Validación en código antes de cada envío

### Reintentos

- Máximo 3 reintentos por defecto
- Los reintentos se procesan al final de cada ejecución diaria
- Los mensajes con más de `max-reintentos` intentos se dejan en estado FALLIDO

---

## 🚀 Producción

### Checklist para Producción

- [ ] Obtener número de WhatsApp Business aprobado
- [ ] Configurar credenciales de producción en Twilio
- [ ] Cambiar `twilio.sandbox=false`
- [ ] Verificar horario del cron según zona horaria del servidor
- [ ] Configurar alertas para mensajes fallidos
- [ ] Implementar monitoreo del servicio

### Variables de Entorno Recomendadas

```bash
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your_auth_token
export TWILIO_WHATSAPP_FROM=whatsapp:+573001234567
```

```properties
twilio.account-sid=${TWILIO_ACCOUNT_SID}
twilio.auth-token=${TWILIO_AUTH_TOKEN}
twilio.whatsapp-from=${TWILIO_WHATSAPP_FROM}
```

---

## 📊 Modelo de Datos

### Tabla: recordatorio_pago

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_recordatorio | INT (PK) | Identificador único |
| id_estudiante | INT (FK) | Referencia al estudiante |
| id_membresia | INT (FK) | Referencia a la membresía |
| tipo_recordatorio | ENUM | Tipo de recordatorio |
| fecha_vencimiento_referencia | DATE | Fecha de vencimiento de la membresía |
| fecha_envio | DATETIME | Fecha y hora del envío |
| mensaje | VARCHAR(500) | Mensaje enviado |
| estado_envio | ENUM | ENVIADO, FALLIDO, PENDIENTE |
| twilio_message_sid | VARCHAR(100) | SID del mensaje en Twilio |
| error_detalle | TEXT | Detalle del error si falló |
| intentos | INT | Número de intentos de envío |

### Índice Único

```sql
UNIQUE (id_membresia, tipo_recordatorio, fecha_vencimiento_referencia)
```

---

## 📞 Soporte

Para problemas con el módulo:
1. Revisar logs de la aplicación
2. Verificar credenciales de Twilio en la consola
3. Usar endpoint `/api/admin/recordatorios/health` para diagnóstico
4. Consultar estadísticas en `/api/admin/recordatorios/estadisticas`
