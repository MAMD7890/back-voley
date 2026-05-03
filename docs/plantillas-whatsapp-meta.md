# Plantillas WhatsApp — Meta Business Manager

Crear cada plantilla en:
**business.facebook.com → WhatsApp → Configuración de la cuenta → Plantillas de mensajes**

Configuración común a todas (excepto `galacticos_prueba`):
- **Categoría**: MARKETING
- **Idioma**: Español (Colombia) `es_CO`

`galacticos_prueba`:
- **Categoría**: UTILITY
- **Idioma**: Español (Colombia) `es_CO`

---

## 1. `galacticos_recordatorio_5_dias`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

Te avisamos con tiempo que tu membresía vence en *5 días*, el *{{fecha}}*.

Renueva antes del vencimiento para continuar disfrutando de los entrenamientos y torneos.

💳 Paga fácil aquí:
{{link}}

¡Gracias por ser parte de la familia Galácticos! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `29/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 2. `galacticos_recordatorio_2_dias`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

Tu membresía vence el *{{fecha}}*, es decir, en *2 días*.

⚠️ Si no renuevas a tiempo perderás el acceso a los entrenamientos.

💳 Renueva ahora:
{{link}}

¡Te esperamos en la cancha! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `29/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 3. `galacticos_vence_hoy`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

🚨 Hoy *{{fecha}}* es el último día de tu membresía.

Para seguir entrenando sin interrupción realiza tu pago hoy mismo.

💳 Pagar ahora:
{{link}}

💡 Si no renuevas hoy, mañana tu estado pasará a mora.
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `27/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 4. `galacticos_mora_1`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

🔴 Tu membresía venció el *{{fecha}}* y tu cuenta está en mora.

Para regularizar tu situación y volver a los entrenamientos realiza tu pago:

💳 Pagar aquí:
{{link}}

¡Te extrañamos en los entrenamientos, renueva y vuelve pronto! 💪
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `26/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 5. `galacticos_mora_2`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

🔴 Han pasado *2 días* desde que tu membresía venció el *{{fecha}}*.

Mientras estés en mora no puedes participar en los entrenamientos.

💳 Regulariza tu pago:
{{link}}

¿Tienes alguna dificultad? Escríbenos, podemos ayudarte. 🤝
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `25/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 6. `galacticos_mora_3`

**Parámetros:** `{{name}}`=nombre · `{{fecha}}`=fecha vencimiento · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

🚨 Tu membresía lleva *3 días vencida* desde el *{{fecha}}*.

⚠️ Tu lugar en el equipo está en riesgo.

Comunícate con el profesor encargado o paga directamente aquí:
{{link}}

¡Queremos que sigas siendo parte del equipo! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `24/04/2026`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 7. `galacticos_bienvenida`

**Parámetros:** `{{name}}`=nombre · `{{equipo}}`=nombre equipo · `{{link}}`=link pago

**Cuerpo:**
```
Hola *{{name}}* 👋

🎉 ¡Bienvenido/a a la Escuela de Voleibol Galácticos!

🏆 Equipo asignado: *{{equipo}}*

Por este medio recibirás recordatorios de pago e información importante del equipo.

💳 Cuando necesites pagar tu membresía:
{{link}}

¡Nos vemos en la cancha! 🌟
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{equipo}}` → `Equipo Cisne`
- `{{link}}` → `https://galacticosvoleysm.com/pagar`

---

## 8. `confirmacionpago`

**Parámetros:** `{{name}}`=nombre · `{{monto}}`=monto · `{{periodo}}`=mes pagado · `{{4}}`=fecha próximo vencimiento

**Cuerpo:**
```
Hola *{{name}}* 👋

✅ ¡Tu pago fue recibido exitosamente!

📋 Detalles:
💰 Monto: ${{monto}}
📅 Período: {{periodo}}
📆 Próximo vencimiento: {{fecha}}

¡Gracias! Tu membresía está al día. Sigue entrenando fuerte. 💪🏐
```

**Ejemplos para revisión de Meta:**
- `{{name}}` → `Sofía Torres`
- `{{fecha}}` → `80000`
- `{{periodo}}` → `Abril 2026`
- `{{fecha}}` → `27/05/2026`

---

## 9. `galacticos_prueba`

**Parámetros:** ninguno

**Cuerpo:**
```
✅ Prueba de conexión exitosa.

El sistema de notificaciones WhatsApp de Galácticos está funcionando correctamente.

¡Gracias por usar nuestro sistema! 🏐
```

**Ejemplos para revisión de Meta:** no aplica

---

## Variables de entorno requeridas en AWS

```
META_PHONE_NUMBER_ID   →  ID del número en WhatsApp Business Manager
META_ACCESS_TOKEN      →  Token permanente del System User
```

## Dónde obtener las credenciales

1. **Phone Number ID**: Meta Business Suite → WhatsApp → Configuración → número de teléfono → copiar ID
2. **Access Token permanente**: Meta Business Suite → Configuración → Usuarios del sistema → crear System User con rol Admin → generar token → seleccionar app y permiso `whatsapp_business_messaging`
