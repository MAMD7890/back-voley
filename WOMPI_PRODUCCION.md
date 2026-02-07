# 🔧 CONFIGURACIÓN WOMPI PARA PRODUCCIÓN

## ⚠️ PASOS PREVIOS

Antes de habilitar Wompi en producción, necesitas:

### 1. Obtener Credenciales de Wompi Producción

Ve a: https://dashboard.wompi.co/

**Datos que necesitas:**
- ✅ Public Key (Producción)
- ✅ Private Key (Producción)
- ✅ Events Secret (Webhooks)
- ✅ Integrity Secret (Firma)

**¿Dónde encontrarlos?**
```
Dashboard → Settings → API Keys → Production
```

---

## 🔄 CAMBIAR DE SANDBOX A PRODUCCIÓN

### Antes (Sandbox/Test)
```properties
wompi.public-key=pub_test_CSA2EFholZpUOQRXltIiXDQixqVK5Rx1
wompi.private-key=prv_test_XpEqYSLxDpNpd9mr3gp2OhEZ6kPCxa8P
wompi.events-secret=test_events_OSPnQ1bcfUdGJj91TjOWeqcVcU6r1oPY
wompi.integrity-secret=test_integrity_M9l4jIsFYxZhOxdGDyUMgh46u6R9kSmq
wompi.api-url=https://sandbox.wompi.co/v1
wompi.sandbox=true
```

### Después (Producción)
```properties
wompi.public-key=pub_prod_tu_public_key_aqui
wompi.private-key=prv_prod_tu_private_key_aqui
wompi.events-secret=prod_events_tu_secret_aqui
wompi.integrity-secret=prod_integrity_tu_secret_aqui
wompi.api-url=https://api.wompi.co/v1
wompi.sandbox=false
```

---

## 📋 CONFIGURACIÓN PASO A PASO

### Paso 1: Actualizar application.properties (Local para test)

```bash
# En tu máquina local, editar:
vim src/main/resources/application.properties

# Cambiar:
wompi.public-key=pub_prod_XXX
wompi.private-key=prv_prod_XXX
wompi.events-secret=prod_events_XXX
wompi.integrity-secret=prod_integrity_XXX
wompi.api-url=https://api.wompi.co/v1
wompi.sandbox=false
```

### Paso 2: Actualizar application-prod.properties (En EC2)

En tu EC2, crear/actualizar:

```bash
# En EC2
sudo nano /opt/galacticos/application-prod.properties

# Agregar/actualizar Wompi:
wompi.public-key=pub_prod_XXX
wompi.private-key=prv_prod_XXX
wompi.events-secret=prod_events_XXX
wompi.integrity-secret=prod_integrity_XXX
wompi.api-url=https://api.wompi.co/v1
wompi.sandbox=false
```

### Paso 3: Compilar y Desplegar

```bash
# Local
mvnw clean package -DskipTests

# Transferir
scp -i ~/galacticos-key.pem target/galacticos-0.0.1-SNAPSHOT.jar \
    ec2-user@3.85.111.48:/opt/galacticos/

# En EC2
sudo systemctl restart galacticos.service
```

---

## ✅ ENDPOINTS WOMPI DISPONIBLES

Todos estos endpoints ya están implementados:

### 1. Generar Firma de Integridad
```bash
GET /api/wompi/integrity-signature?amount=50000&reference=REF123&currency=COP

# Respuesta:
{
  "reference": "REF123",
  "amount": 50000,
  "currency": "COP",
  "integritySignature": "8a4b9c...",
  "publicKey": "pub_prod_..."
}
```

### 2. Crear Link de Pago
```bash
POST /api/wompi/create-payment-link
Content-Type: application/json

{
  "description": "Pago de membresía - Enero 2024",
  "amountInCents": 5000000,
  "customerEmail": "usuario@example.com",
  "customerName": "Juan Pérez",
  "reference": "PAG-001-2024",
  "redirectUrl": "https://d2ga9msb3312dv.cloudfront.net/pago-exitoso"
}

# Respuesta:
{
  "success": true,
  "paymentLinkUrl": "https://checkout.wompi.co/l/...",
  "transactionId": "..."
}
```

### 3. Verificar Estado de Transacción
```bash
GET /api/wompi/transaction/TRANSACTION_ID

# Respuesta:
{
  "id": "TRANSACTION_ID",
  "status": "APPROVED",
  "amount": 5000000,
  "reference": "PAG-001-2024",
  "paymentMethod": "CARD",
  "timestamp": "2024-02-07T13:00:00Z"
}
```

### 4. Webhook de Eventos
```bash
POST /api/wompi/webhook
Content-Type: application/json

{
  "event": "transaction.updated",
  "data": {
    "transaction": {
      "id": "TRANS_ID",
      "status": "APPROVED",
      "reference": "PAG-001-2024"
    }
  },
  "timestamp": 1707306000
}

# Automáticamente:
# ✅ Valida la firma de eventos
# ✅ Actualiza el estado del pago en BD
# ✅ Procesa el pago
```

---

## 🧪 TESTING EN PRODUCCIÓN

### Test 1: Generar Firma
```bash
curl -X GET "http://3.85.111.48:8080/api/wompi/integrity-signature?amount=50000&reference=TEST123&currency=COP" \
  -H "Content-Type: application/json"

# ✅ Debe retornar firma válida
```

### Test 2: Crear Link de Pago
```bash
curl -X POST http://3.85.111.48:8080/api/wompi/create-payment-link \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Test Pago",
    "amountInCents": 100000,
    "customerEmail": "test@example.com",
    "customerName": "Test User",
    "reference": "TEST-001",
    "redirectUrl": "https://d2ga9msb3312dv.cloudfront.net/success"
  }'

# ✅ Debe retornar URL de checkout
```

### Test 3: Verificar Transacción
```bash
curl -X GET http://3.85.111.48:8080/api/wompi/transaction/TRANS_ID \
  -H "Content-Type: application/json"

# ✅ Debe retornar estado de transacción
```

---

## 🔐 SEGURIDAD EN PRODUCCIÓN

### ✅ Lo que ya está implementado:

1. **Validación de Firma de Eventos**
   - Verifica que los webhooks vengan realmente de Wompi
   - Usa events-secret para validar

2. **Validación de Integridad**
   - Firma cada pago con integrity-secret
   - Previene manipulación de datos

3. **Encriptación**
   - Private Key nunca se expone al frontend
   - Solo Public Key se envía al cliente

4. **Validación de Amount**
   - Verifica que el monto no cambió
   - Previene fraud

---

## 🔗 INTEGRACIÓN CON FRONTEND

### En tu Frontend (CloudFront)

```javascript
// 1. Obtener firma de integridad
const response = await fetch('http://3.85.111.48:8080/api/wompi/integrity-signature', {
  params: {
    amount: 50000,
    reference: 'REF123',
    currency: 'COP'
  }
});

const { integritySignature, publicKey } = await response.json();

// 2. Inicializar Widget de Wompi
const wompi = new WidgetCheckout({
  currency: 'COP',
  amountInCents: 50000,
  reference: 'REF123',
  publicKey: publicKey,
  integritySignature: integritySignature,
  redirectUrl: 'https://d2ga9msb3312dv.cloudfront.net/pago-exitoso'
});

wompi.render('#checkout-container');

// 3. El widget maneja el pago automáticamente
```

---

## 📊 FLUJO COMPLETO DE PAGO

```
1. Usuario abre página de pago en CloudFront
   ↓
2. Frontend solicita firma a API
   GET /api/wompi/integrity-signature
   ↓
3. API retorna firma + Public Key
   ↓
4. Frontend renderiza Widget de Wompi
   ↓
5. Usuario ingresa datos de tarjeta
   ↓
6. Widget envía pago a Wompi
   ↓
7. Wompi procesa pago
   ↓
8. Wompi envía webhook a API
   POST /api/wompi/webhook
   ↓
9. API valida webhook y actualiza BD
   ↓
10. Usuario es redirigido a /pago-exitoso
    ↓
11. Frontend verifica pago en BD
    ↓
12. ✅ Pago completado
```

---

## 🚨 CHECKLIST PRE-PRODUCCIÓN

- [ ] Obtuviste credenciales de Wompi Producción
- [ ] Actualizaste application.properties localmente
- [ ] Actualizaste application-prod.properties en EC2
- [ ] Compilaste JAR con nuevas credenciales
- [ ] Desplegaste JAR en EC2
- [ ] Reiniciaste servicio
- [ ] Probaste /api/wompi/integrity-signature
- [ ] Probaste /api/wompi/create-payment-link
- [ ] Probaste webhook desde Wompi Dashboard
- [ ] Frontend genera pagos correctamente
- [ ] BD actualiza estado de pagos

---

## 📝 VARIANTES DE PAGO

El sistema soporta:

```java
// Tarjeta de Crédito
CARD

// Transferencia Bancaria
BANK_TRANSFER

// Billetera Digital
DIGITAL_WALLET

// Efectivo (Si Wompi lo soporta)
CASH

// QR (Si Wompi lo soporta)
QR
```

---

## 💾 ALMACENAMIENTO DE PAGOS

Los pagos se guardan en la tabla `pago`:

```sql
SELECT * FROM pago WHERE id_estudiante = 1;

-- Columnas importantes:
-- id_pago: ID único
-- id_estudiante: A quién se le cobra
-- monto: Cantidad en COP
-- estado_pago: PENDIENTE, AL_DIA, EN_MORA, COMPROMISO_PAGO
-- metodo_pago: WOMPI, EFECTIVO, TRANSFERENCIA
-- referencia_pago: ID de transacción en Wompi
-- fecha_pago: Cuándo se pagó
```

---

## 🔄 RECONCILIACIÓN DE PAGOS

Para reconciliar pagos:

```bash
# Obtener todos los pagos de Wompi pendientes
SELECT * FROM pago WHERE metodo_pago = 'WOMPI' AND estado_pago = 'PENDIENTE';

# Para cada uno, consultar estado
curl -X GET http://3.85.111.48:8080/api/wompi/transaction/TRANS_ID

# Si status = APPROVED
# → Actualizar estado en BD a AL_DIA
# → Enviar email de confirmación
```

---

## 🆘 TROUBLESHOOTING

### "Error de firma"
- Verifica que integrity-secret es correcto
- Verifica que el reference está bien formado

### "Transaction not found"
- El ID de transacción debe venir de Wompi
- Verifica en Wompi Dashboard que existe

### "Webhook no llega"
- Verifica que events-secret es correcto
- En Wompi Dashboard → Webhooks → Test
- Revisa logs en EC2: `sudo journalctl -u galacticos.service -f`

### "Public key inválida"
- Usa la PUBLIC KEY en producción, no sandbox
- Verifica que no tiene espacios extra

---

## 📞 SOPORTE WOMPI

- Documentación: https://docs.wompi.co
- Dashboard: https://dashboard.wompi.co
- API Reference: https://docs.wompi.co/api
- Test Data: https://docs.wompi.co/testing

---

## ✅ STATUS

```
✅ Código: WompiService.java implementado
✅ Endpoints: Todos disponibles
✅ Webhooks: Configurados
✅ Firma: Validación lista
✅ Seguridad: Protegido

⏳ Pasos necesarios:
  1. Obtener credenciales de Wompi
  2. Actualizar properties
  3. Compilar y desplegar
  4. Probar endpoints
  5. ¡Listo para producción!
```

**¡Wompi está listo para producción!** 🚀

