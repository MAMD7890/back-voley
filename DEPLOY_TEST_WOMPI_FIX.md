# 🚀 DEPLOY & TEST - Wompi Payment Link Fix

## 📦 Pasos de Compilación y Despliegue

### 1. Compilar Cambios

```bash
# Limpiar y compilar
mvn clean compile

# Si todo está bien, empaquetar
mvn clean package -DskipTests
```

**Cambios de código compilados:**
- `WompiPaymentLinkResponse.java` - DTO simplificado
- `WompiService.java` - Builder actualizado (líneas 110-122)

---

## 🧪 Testing del Endpoint

### Test 1: Crear Payment Link

**Endpoint:** `POST /api/wompi/payment-link`

**Request:**
```bash
curl -X POST http://localhost:8080/api/wompi/payment-link \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <tu_token_jwt>" \
  -d '{
    "idEstudiante": 1,
    "amount": 500.00,
    "currency": "COP",
    "customerEmail": "estudiante@example.com",
    "customerName": "Juan Pérez García",
    "mesPagado": "ENERO_2025"
  }'
```

**Response Esperada (200 OK):**
```json
{
  "success": true,
  "paymentLinkUrl": "https://checkout.wompi.co/l/link_zQxYwVuTsRpQoNm",
  "reference": "REF-1-ENERO_2025-1704067200",
  "id": "link_zQxYwVuTsRpQoNm",
  "message": "Link de pago creado exitosamente",
  "amountInCents": 50000,
  "currency": "COP"
}
```

**Verificar:**
1. ✅ `success` es `true`
2. ✅ `paymentLinkUrl` comienza con `https://checkout.wompi.co/l/`
3. ✅ `reference` sigue el formato `REF-{id}-{mes}-{timestamp}`
4. ✅ `id` es un UUID válido de Wompi
5. ✅ NO hay campos innecesarios como `name`, `description`, `singleUse`

---

### Test 2: Verificar BD - Pago Guardado

**Query SQL:**
```sql
SELECT * FROM pago 
WHERE referencia_pago LIKE 'REF-1-ENERO_2025-%' 
ORDER BY id_pago DESC 
LIMIT 1;
```

**Verificar:**
- ✅ El pago existe en la BD
- ✅ `estado_pago` = `PENDIENTE`
- ✅ `metodo_pago` = `ONLINE`
- ✅ `valor` = 500.00
- ✅ `mes_pagado` = `ENERO_2025`
- ✅ `wompi_transaction_id` está vacío (se llenará cuando Wompi confirme)

---

### Test 3: Flujo Completo en Frontend

**Código JavaScript/React:**
```javascript
async function handleWompiPayment() {
  try {
    // 1. Crear payment link
    const response = await fetch('http://localhost:8080/api/wompi/payment-link', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        idEstudiante: studentId,
        amount: 500.00,
        currency: 'COP',
        customerEmail: 'estudiante@example.com',
        customerName: 'Test User',
        mesPagado: 'ENERO_2025'
      })
    });

    const paymentData = await response.json();

    if (paymentData.success) {
      console.log('✅ Payment link creado:', paymentData.paymentLinkUrl);
      
      // 2. Redirigir a Wompi
      window.location.href = paymentData.paymentLinkUrl;
      
      // NO hacer esto:
      // - NO inicializar widget
      // - NO usar publicKey
      // - NO usar signature
      // - NO usar amountInCents (excepto para logging)
    } else {
      console.error('❌ Error creando link:', paymentData.message);
    }
  } catch (error) {
    console.error('❌ Error:', error);
  }
}
```

**Pasos:**
1. Llamar endpoint `/api/wompi/payment-link`
2. Obtener respuesta con `paymentLinkUrl`
3. Redirigir: `window.location.href = response.paymentLinkUrl`
4. Usuario ve página de Wompi
5. Usuario completa pago
6. Wompi redirige a URL configurada (success/failure)
7. Backend procesa webhook y actualiza BD

---

## 📊 Verificación Post-Despliegue

### Checklist de Verificación

- [ ] **Compilación:** Proyecto compila sin errores
- [ ] **DTO Simplificado:** WompiPaymentLinkResponse solo tiene 7 campos
- [ ] **Endpoint Funciona:** GET `paymentLinkUrl` con formato correcto
- [ ] **BD Guardado:** Pago existe con estado PENDIENTE
- [ ] **Redirect Funciona:** Usuario puede acceder a checkout.wompi.co
- [ ] **Webhook Procesa:** Transacción se confirma después del pago

### Tests en Postman

**Environment Variable:**
```
base_url = http://localhost:8080
token = <tu_jwt_token>
```

**Test 1 - Crear Payment Link:**
```
POST {{base_url}}/api/wompi/payment-link
Authorization: Bearer {{token}}

{
  "idEstudiante": 1,
  "amount": 500,
  "currency": "COP",
  "customerEmail": "test@test.com",
  "customerName": "Test User",
  "mesPagado": "ENERO_2025"
}

// Verificar status 200
// Verificar paymentLinkUrl existe
// Verificar success = true
```

**Test 2 - Obtener Pagos del Estudiante:**
```
GET {{base_url}}/api/pagos/estudiante/1
Authorization: Bearer {{token}}

// Verificar que el pago nuevo aparece con estado PENDIENTE
```

---

## 🔍 Debug - Si Algo Falla

### Error: "Cannot read properties of undefined"
- **Causa:** Frontend intenta usar respuesta como widget
- **Solución:** Actualizar frontend para redirigir a `paymentLinkUrl`

### Error: "Error al crear el link de pago"
- **Causa:** Wompi API rechazó la solicitud
- **Debug:**
  1. Verificar claves de Wompi en `application.properties`
  2. Verificar que `wompi.sandbox=false`
  3. Ver logs: `tail -f logs/application.log | grep Wompi`

### Error: "Pago no guardado en BD"
- **Causa:** `createPendingPayment()` falló
- **Debug:**
  1. Verificar que estudiante existe: `SELECT * FROM estudiante WHERE id_estudiante = 1`
  2. Ver logs para excepciones en estudiante repository
  3. Verificar FK_estudiante es válida

### Error: "Invalid token"
- **Causa:** Token JWT expirado o inválido
- **Solución:** 
  1. Autenticarse de nuevo
  2. Obtener nuevo token
  3. Usar en header: `Authorization: Bearer <nuevo_token>`

---

## 📋 Logs a Monitorear

**En servidor/consola Java, buscar:**

```
✅ Log esperado - Éxito:
[INFO] Creando link de pago para estudiante: 1
[INFO] Pago pendiente creado - Estudiante: 1, Referencia: REF-1-ENERO_2025-...

❌ Log esperado - Error:
[ERROR] Error creando link de pago en Wompi: ...
[ERROR] Estudiante no encontrado
```

**En servidor Wompi, buscar:**
```
Evento: transaction.updated
Status: APPROVED (éxito) | DECLINED (rechazado)
```

---

## 🌐 URLs de Redirección

**Después del pago exitoso, Wompi redirige a:**
```
http://localhost:8080/api/wompi/payment-result?id=prod_XXX&env=PROD&reference=REF-XXX
```

**Este endpoint ya existe** en `WompiController.java` línea 174

---

## 📝 Resumen de Cambios Compilados

```
✅ WompiPaymentLinkResponse.java
   - Antes: 13 campos (nombre, descripción, singleUse, active, expiresAt, etc.)
   - Después: 7 campos (success, paymentLinkUrl, reference, id, message, amountInCents, currency)
   
✅ WompiService.java (líneas 110-122)
   - Simplificado builder de respuesta
   - Removidos: .name(), .description(), .singleUse(), .active()
   - Mantenidos: .success(), .id(), .paymentLinkUrl(), .reference()
```

---

## ✨ Validación Final

**Después de desplegar, el flujo debe ser:**

1. ✅ Frontend: `POST /api/wompi/payment-link`
2. ✅ Backend: Guarda pago con estado PENDIENTE
3. ✅ Backend: Retorna `{success, paymentLinkUrl, reference, id}`
4. ✅ Frontend: Redirige a `paymentLinkUrl`
5. ✅ Usuario: Ve página de Wompi
6. ✅ Usuario: Completa pago
7. ✅ Wompi: Envía webhook
8. ✅ Backend: Procesa webhook y actualiza estado a PAGADO
9. ✅ BD: Estado del pago = PAGADO, estado estudiante = AL_DIA

---

## 🆘 Soporte

Si encuentras problemas:
1. Verifica logs en `target/logs/` o consola
2. Valida que `application.properties` tiene credenciales correctas
3. Asegúrate que JWT token es válido
4. Verifica que estudiante existe en BD
5. Verifica que credenciales de Wompi son de PRODUCCIÓN (no sandbox)
