# ✅ CHECKLIST DEPLOYMENT PRODUCCIÓN - WOMPI + CLOUDFRONT

## 🎯 Pre-Deployment

### Configuración Backend
- [ ] **Obtener credenciales Wompi producción**
  - [ ] Public Key (pub_prod_*)
  - [ ] Private Key (prv_prod_*)
  - [ ] Integrity Secret (prod_integrity_*)
  - [ ] Events Secret (prod_events_*)
  - Sitio: https://dashboard.wompi.co/settings/api-keys

- [ ] **Actualizar application-prod.properties en EC2**
  ```bash
  # SSH a EC2
  ssh -i tu-clave.pem ec2-user@3.85.111.48
  
  # Editar archivo
  sudo nano /opt/galacticos/application-prod.properties
  
  # Actualizar:
  wompi.public-key=pub_prod_xxxxx
  wompi.private-key=prv_prod_xxxxx
  wompi.integrity-secret=prod_integrity_xxxxx
  wompi.events-secret=prod_events_xxxxx
  wompi.sandbox=false
  ```

- [ ] **Compilar JAR localmente**
  ```bash
  mvnw.cmd clean package -DskipTests
  # Genera: target/galacticos-0.0.1-SNAPSHOT.jar (68 MB)
  ```

- [ ] **Transferir JAR a EC2**
  ```bash
  scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar ec2-user@3.85.111.48:/tmp/
  
  # En EC2:
  sudo mv /tmp/galacticos-0.0.1-SNAPSHOT.jar /opt/galacticos/
  sudo chown galacticos:galacticos /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
  ```

- [ ] **Reiniciar servicio**
  ```bash
  sudo systemctl restart galacticos.service
  ```

- [ ] **Verificar que el servicio está corriendo**
  ```bash
  sudo systemctl status galacticos.service
  ```

### Configuración Frontend (CloudFront)
- [ ] **Verificar CloudFront está apuntando a CloudFront Distribution**
  - Distribución: d2ga9msb3312dv.cloudfront.net
  - Origin: S3 o ALB según configuración

- [ ] **Configurar CORS en S3 (si usa S3)**
  ```json
  [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
      "AllowedOrigins": ["https://d2ga9msb3312dv.cloudfront.net"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
  ```

- [ ] **Variables de entorno frontend**
  ```javascript
  // src/environments/environment.prod.ts
  export const environment = {
    production: true,
    apiUrl: 'https://3.85.111.48:8080',  // o nip.io domain
    apiBaseUrl: 'https://api.voley.com'   // si tienes dominio
  };
  ```

- [ ] **Build frontend para producción**
  ```bash
  npm run build:prod
  # o
  yarn build:prod
  ```

- [ ] **Deploy frontend a S3/CloudFront**
  ```bash
  # Subir archivos a S3
  aws s3 sync dist/ s3://tu-bucket/ --delete
  
  # Invalidar caché de CloudFront
  aws cloudfront create-invalidation \
    --distribution-id E1234567890ABC \
    --paths "/*"
  ```

---

## 🧪 Testing Post-Deployment

### Test 1: Autenticación
```bash
# Registrar usuario
curl -X POST http://3.85.111.48:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TuContraseña123",
    "nombre": "Test User"
  }'

# Login
curl -X POST http://3.85.111.48:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TuContraseña123"
  }'
```

**Resultado esperado:** Token JWT con estado 200 OK

### Test 2: Firma de Integridad (Wompi)
```bash
curl -X GET "http://3.85.111.48:8080/api/wompi/integrity-signature?amount=5000000&reference=TEST-001&currency=COP" \
  -H "Content-Type: application/json"
```

**Resultado esperado:**
```json
{
  "publicKey": "pub_prod_xxxxx",
  "integritySignature": "signature_hash_aqui",
  "reference": "TEST-001",
  "amount": 5000000
}
```

### Test 3: Crear Link de Pago
```bash
curl -X POST http://3.85.111.48:8080/api/wompi/create-payment-link \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Test Payment",
    "amountInCents": 5000000,
    "customerEmail": "test@example.com",
    "customerName": "Test User",
    "reference": "TEST-001",
    "redirectUrl": "https://d2ga9msb3312dv.cloudfront.net/pago-exitoso"
  }'
```

**Resultado esperado:**
```json
{
  "success": true,
  "paymentLinkUrl": "https://checkout.wompi.co/...",
  "reference": "TEST-001"
}
```

### Test 4: Webhook de Wompi
```bash
# Simular webhook (en EC2)
curl -X POST http://localhost:8080/api/wompi/webhook \
  -H "Content-Type: application/json" \
  -H "X-Wompi-Signature: firma_aqui" \
  -d '{
    "event": "transaction.updated",
    "data": {
      "id": "123456",
      "status": "APPROVED",
      "reference": "TEST-001",
      "amount_in_cents": 5000000
    }
  }'
```

**Resultado esperado:** 200 OK con confirmación

### Test 5: CORS desde CloudFront
```bash
curl -X OPTIONS http://3.85.111.48:8080/api/auth/login \
  -H "Origin: https://d2ga9msb3312dv.cloudfront.net" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

**Resultado esperado:** Headers CORS con `Access-Control-Allow-Origin`

---

## 🔐 Seguridad

- [ ] **HTTPS/SSL en EC2**
  ```bash
  # Instalar Let's Encrypt
  sudo yum install certbot python3-certbot-nginx
  
  # Generar certificado
  sudo certbot certonly --standalone -d api.voley.com
  ```

- [ ] **Nginx con SSL**
  ```nginx
  server {
    listen 443 ssl http2;
    server_name 3.85.111.48;

    ssl_certificate /etc/letsencrypt/live/api.voley.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.voley.com/privkey.pem;

    location / {
      proxy_pass http://localhost:8080;
    }
  }
  ```

- [ ] **WAF en CloudFront**
  - Enable AWS WAF
  - Rate limiting
  - SQL injection protection
  - XSS protection

- [ ] **Credenciales seguras**
  - [ ] Wompi keys en `/opt/galacticos/application-prod.properties` (NO en código)
  - [ ] JWT secret fuerte (>32 caracteres)
  - [ ] Database password cambiada
  - [ ] RDS con encryption habilitada

---

## 📊 Monitoreo

- [ ] **CloudWatch Logs**
  ```bash
  # Ver logs de la aplicación
  tail -f /var/log/galacticos/application.log
  ```

- [ ] **Alarmas CloudWatch**
  - High CPU usage (>80%)
  - High memory usage (>80%)
  - Errores en aplicación
  - Fallos de pago

- [ ] **Métricas de Pago**
  - Total de pagos procesados
  - Pagos aprobados vs rechazados
  - Monto total procesado
  - Tasa de error en webhooks

---

## 🚀 Rollback Plan

Si algo sale mal:

```bash
# Detener servicio
sudo systemctl stop galacticos.service

# Restaurar JAR anterior
sudo cp /opt/galacticos/backup/galacticos-0.0.1-SNAPSHOT.jar.bak /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar

# Restaurar propiedades
sudo cp /opt/galacticos/backup/application-prod.properties.bak /opt/galacticos/application-prod.properties

# Reiniciar
sudo systemctl start galacticos.service

# Verificar
sudo systemctl status galacticos.service
```

---

## 📝 Documentación Generada

✅ Creados durante la configuración:

1. **WOMPI_FRONTEND_INTEGRACION.md** - Guía para implementar Wompi en frontend
2. **WOMPI_PRODUCCION.md** - Guía de credenciales y configuración
3. **CLOUDFRONT_CORS_ACTUALIZADO.md** - Configuración CORS para CloudFront
4. **application-prod.properties.template** - Template de configuración

---

## 📞 Contactos de Soporte

- **Wompi Soporte**: https://women.wompi.co/es/
- **Wompi Docs**: https://docs.wompi.co
- **AWS Support**: https://console.aws.amazon.com/support
- **CloudFront Console**: https://console.aws.amazon.com/cloudfront

---

## 🎉 Confirmación Final

Después de completar todos los checks:

```bash
# Test final completo
echo "✅ Backend configurado"
echo "✅ Wompi producción habilitado"
echo "✅ CloudFront CORS funcionando"
echo "✅ Frontend desplegado"
echo "✅ Webhooks recibiendo eventos"
echo "✅ Monitoreo activo"
echo ""
echo "🎉 ¡Sistema en Producción!"
```

---

**Versión:** 1.0 | **Última actualización:** 2024
**Estado:** ✅ Listo para Producción

