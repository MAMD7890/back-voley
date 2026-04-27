# Plantillas WhatsApp — Meta Business Manager

Crear cada plantilla en:
**business.facebook.com → WhatsApp → Configuración de la cuenta → Plantillas de mensajes**

Configuración común a todas:
- **Categoría**: UTILITY
- **Idioma**: Español (Colombia) `es_CO`
- **Tipo de mensaje**: Marketing / Utilidad

---

## 1. `galacticos_recordatorio_5_dias`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

Te avisamos con tiempo que tu membresía vence en *5 días*, el *{{2}}*.

Renueva antes del vencimiento para continuar disfrutando de los entrenamientos y torneos.

💳 Paga fácil aquí:
{{3}}

¡Gracias por ser parte de la familia Galácticos! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `29/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 2. `galacticos_recordatorio_2_dias`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

Tu membresía vence el *{{2}}*, es decir, en *2 días*.

⚠️ Si no renuevas a tiempo perderás el acceso a los entrenamientos.

💳 Renueva ahora:
{{3}}

¡Te esperamos en la cancha! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `29/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 3. `galacticos_vence_hoy`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

🚨 Hoy *{{2}}* es el último día de tu membresía.

Para seguir entrenando sin interrupción realiza tu pago hoy mismo.

💳 Pagar ahora:
{{3}}

💡 Si no renuevas hoy, mañana tu estado pasará a mora.
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `27/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 4. `galacticos_mora_1`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

🔴 Tu membresía venció el *{{2}}* y tu cuenta está en mora.

Para regularizar tu situación y volver a los entrenamientos realiza tu pago:

💳 Pagar aquí:
{{3}}

¡Te extrañamos en los entrenamientos, renueva y vuelve pronto! 💪
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `26/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 5. `galacticos_mora_2`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

🔴 Han pasado *2 días* desde que tu membresía venció el *{{2}}*.

Mientras estés en mora no puedes participar en los entrenamientos.

💳 Regulariza tu pago:
{{3}}

¿Tienes alguna dificultad? Escríbenos, podemos ayudarte. 🤝
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `25/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 6. `galacticos_mora_3`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=fecha vencimiento · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

🚨 Tu membresía lleva *3 días vencida* desde el *{{2}}*.

⚠️ Tu lugar en el equipo está en riesgo.

Comunícate con el profesor encargado o paga directamente aquí:
{{3}}

¡Queremos que sigas siendo parte del equipo! 🏐
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `24/04/2026`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 7. `galacticos_bienvenida`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=nombre equipo · `{{3}}`=link pago

**Cuerpo:**
```
Hola *{{1}}* 👋

🎉 ¡Bienvenido/a a la Escuela de Voleibol Galácticos!

🏆 Equipo asignado: *{{2}}*

Por este medio recibirás recordatorios de pago e información importante del equipo.

💳 Cuando necesites pagar tu membresía:
{{3}}

¡Nos vemos en la cancha! 🌟
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `Equipo Cisne`
- `{{3}}` → `https://galacticosvoleysm.com/pagar`

---

## 8. `galacticos_confirmacion_pago`

**Parámetros:** `{{1}}`=nombre · `{{2}}`=monto · `{{3}}`=mes pagado · `{{4}}`=fecha próximo vencimiento

**Cuerpo:**
```
Hola *{{1}}* 👋

✅ ¡Tu pago fue recibido exitosamente!

📋 Detalles:
💰 Monto: ${{2}}
📅 Período: {{3}}
📆 Próximo vencimiento: {{4}}

¡Gracias! Tu membresía está al día. Sigue entrenando fuerte. 💪🏐
```

**Ejemplos para revisión de Meta:**
- `{{1}}` → `Sofía Torres`
- `{{2}}` → `80000`
- `{{3}}` → `Abril 2026`
- `{{4}}` → `27/05/2026`

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
