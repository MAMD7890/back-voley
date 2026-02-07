# ⚡ INICIO RÁPIDO - WOMPI PRODUCCIÓN

**Tiempo estimado:** 30 minutos | **Dificultad:** ⭐⭐⭐ Media | **Estado:** ✅ Listo

---

## 🎯 ¿QUÉ NECESITO HACER AHORA?

### Opción A: SI SÓ YA ESTÁ DESPLEGADO EN EC2 (RÁPIDO)

```bash
# Solo necesitas actualizar las credenciales de Wompi

1️⃣  OBTÉN CREDENCIALES (2 min)
   Sitio: https://dashboard.wompi.co/settings/api-keys
   Copia:
   - pub_prod_xxxxxxxxxxxxx
   - prv_prod_xxxxxxxxxxxxx
   - prod_integrity_xxxxxxxxxx
   - prod_events_xxxxxxxxxxxxxx

2️⃣  ACTUALIZA EN EC2 (5 min)
   ssh -i tu-clave.pem ec2-user@3.85.111.48
   sudo nano /opt/galacticos/application-prod.properties
   
   # Cambia estas líneas:
   wompi.public-key=pub_prod_xxxxxxxxxxxxx
   wompi.private-key=prv_prod_xxxxxxxxxxxxx
   wompi.integrity-secret=prod_integrity_xxxxxxxxxx
   wompi.events-secret=prod_events_xxxxxxxxxxxxxx
   wompi.sandbox=false
   
   # Ctrl+O, Enter, Ctrl+X (guardar)

3️⃣  REINICIA SERVICIO (1 min)
   sudo systemctl restart galacticos.service
   
4️⃣  VERIFICA (1 min)
   sudo systemctl status galacticos.service
   
   # Si dice "active (running)" ✅ 
```

---

### Opción B: SI NECESITAS COMPILAR Y DESPLEGAR (30 min)

```bash
# Debes compilar, subir JAR y actualizar credenciales

1️⃣  OBTÉN CREDENCIALES (2 min)
   https://dashboard.wompi.co/settings/api-keys
   Copia los 4 valores (pub_prod_*, prv_prod_*, etc.)

2️⃣  ACTUALIZA TEMPLATE LOCALMENTE (2 min)
   Abre: application-prod.properties.template
   Actualiza sección [WOMPI]:
   wompi.public-key=pub_prod_xxxxx
   wompi.private-key=prv_prod_xxxxx
   wompi.integrity-secret=prod_integrity_xxxxx
   wompi.events-secret=prod_events_xxxxx
   Guarda el archivo

3️⃣  COMPILA (5 min)
   mvnw.cmd clean package -DskipTests
   
   Espera a que termina (debe decir "BUILD SUCCESS")

4️⃣  DEPLOY AUTOMÁTICO (10 min)
   bash deploy-produccion.sh
   
   Selecciona opción: 1 (Deployment completo)
   
   El script va a:
   ✅ Hacer backup del anterior
   ✅ Subir el nuevo JAR
   ✅ Reiniciar el servicio
   ✅ Verificar que funciona

5️⃣  VERIFICA (5 min)
   # Test de autenticación
   curl -X POST http://3.85.111.48:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@test.com","password":"pass123"}'
   
   # Test de Wompi
   curl "http://3.85.111.48:8080/api/wompi/integrity-signature?amount=5000000&reference=TEST&currency=COP"
   
   Si ambos retornan datos ✅
```

---

## 📋 CHECKLIST DE VERIFICACIÓN (5 MINUTOS)

Después de completar cualquiera de las opciones arriba, verifica esto:

```bash
# 1. ¿Servicio está corriendo?
sudo systemctl status galacticos.service
# Resultado esperado: "active (running)"

# 2. ¿Frontend puede conectarse?
curl -X OPTIONS http://3.85.111.48:8080/api/auth/login \
  -H "Origin: https://d2ga9msb3312dv.cloudfront.net"
# Resultado esperado: Headers CORS presentes

# 3. ¿Wompi está configurado?
curl "http://3.85.111.48:8080/api/wompi/integrity-signature?amount=5000000&reference=TEST&currency=COP"
# Resultado esperado: JSON con publicKey y integritySignature

# 4. ¿Base de datos está conectada?
ssh -i tu-clave.pem ec2-user@3.85.111.48
sudo tail -f /var/log/galacticos/application.log | grep -i "database\|mysql"
# Resultado esperado: Connection successful o similar
```

---

## 🧪 PRIMER PAGO DE PRUEBA (10 MINUTOS)

Después de verificar arriba, prueba un pago real:

### En el Frontend:

```javascript
// 1. Usuario intenta pagar
// 2. Frontend llama endpoint:

fetch('http://3.85.111.48:8080/api/wompi/integrity-signature', {
  method: 'GET',
  params: {
    amount: 5000,        // 50 COP
    reference: 'TEST-001',
    currency: 'COP'
  }
})
.then(r => r.json())
.then(data => {
  console.log('✅ Firma obtenida:', data)
  // Abre widget de Wompi aquí
})
.catch(e => {
  console.error('❌ Error:', e.message)
  // Revisa logs en EC2: sudo tail -f /var/log/galacticos/application.log
})
```

### Tarjeta de Prueba (Wompi Producción):

Para PROBAR SIN gastar dinero, usa estas tarjetas en ambiente sandbox:

```
❌ NO USES EN PRODUCCIÓN
❌ Solo para testing en sandbox

Tarjeta de Prueba: 4242 4242 4242 4242
Expiración: 12/25
CVV: 123

Resultado esperado: APROBADO ✅

Nota: Si ya cambié a wompi.sandbox=false (producción),
estas tarjetas NO van a funcionar.
Para probar en PRODUCCIÓN, necesitas una tarjeta REAL.
```

---

## 🚨 PROBLEMAS COMUNES Y SOLUCIONES

### Problema 1: "401 Unauthorized" al hacer login

```bash
# Solución 1: Verificar CORS
curl -X OPTIONS http://3.85.111.48:8080/api/auth/login \
  -H "Origin: https://d2ga9msb3312dv.cloudfront.net" \
  -H "Access-Control-Request-Method: POST" \
  -v

# Si falta "Access-Control-Allow-Origin", revisar SecurityConfig.java
# Este ya está arreglado en la versión actual ✅

# Solución 2: Verificar JWT Secret
grep jwt.secret /opt/galacticos/application-prod.properties

# Solución 3: Revisar logs
sudo tail -f /var/log/galacticos/application.log | grep -i error
```

### Problema 2: Wompi no responde

```bash
# Solución 1: ¿Credenciales están en application-prod.properties?
ssh -i tu-clave.pem ec2-user@3.85.111.48
grep wompi.public-key /opt/galacticos/application-prod.properties
# Resultado esperado: wompi.public-key=pub_prod_xxxxx (NO pub_test_)

# Solución 2: ¿wompi.sandbox está en false?
grep wompi.sandbox /opt/galacticos/application-prod.properties
# Resultado esperado: wompi.sandbox=false

# Solución 3: ¿La URL de API es correcta?
grep wompi.api-url /opt/galacticos/application-prod.properties
# Resultado esperado: wompi.api-url=https://api.wompi.co

# Solución 4: Ver logs de error
sudo tail -100 /var/log/galacticos/application.log | grep -i wompi
```

### Problema 3: Webhook no se recibe

```bash
# Solución 1: ¿Firewall permite incoming?
sudo ufw status
# Resultado esperado: 8080/tcp ALLOW

# Solución 2: ¿Nginx está forward correctamente?
sudo systemctl status nginx
# Resultado esperado: active (running)

# Solución 3: Ver webhook en Dashboard Wompi
# https://dashboard.wompi.co → Webhooks → Event logs
# Verifica si los eventos aparecen ahí

# Solución 4: Verificar URL webhook en Wompi:
# https://dashboard.wompi.co → Webhooks → URL
# Debe ser: https://3.85.111.48:8080/api/wompi/webhook
# O tu dominio si lo tiene
```

### Problema 4: JAR no compila

```bash
# Si ves errores en: mvnw.cmd clean package -DskipTests

# Solución 1: Limpiar cache
mvnw.cmd clean
del /s /q target

# Solución 2: Verificar Java version
java -version
# Resultado esperado: java version "17" o mayor

# Solución 3: Si fallan tests
mvnw.cmd package -DskipTests  # <-- El DskipTests es importante

# Solución 4: Ver error completo
# El error debe estar en los últimos 50 líneas de output
```

---

## 📊 DASHBOARDS IMPORTANTES

Guarda estos links en favoritos:

```
Wompi Dashboard (Monitorear pagos):
https://dashboard.wompi.co

Wompi API Keys (Obtener credenciales):
https://dashboard.wompi.co/settings/api-keys

Wompi Webhooks (Ver eventos recibidos):
https://dashboard.wompi.co → Webhooks → Event logs

AWS EC2 Console (Ver servidor):
https://console.aws.amazon.com/ec2/v2/home

CloudFront Console (Ver distribución):
https://console.aws.amazon.com/cloudfront/v3/home

RDS Console (Ver base de datos):
https://console.aws.amazon.com/rds/home
```

---

## 🎯 CONFIGURACIÓN FINAL RECOMENDADA

Después de que todo funciona, actualiza estos valores en application-prod.properties:

```properties
# SEGURIDAD (CAMBIAR ESTOS)
jwt.secret=TU_CLAVE_SEGURA_DE_32_CARACTERES_AQUI
spring.datasource.password=TU_PASSWORD_BD_FUERTE

# LOGGING (PARA PRODUCCIÓN)
logging.level.root=WARN
logging.level.galacticos_app_back=INFO
logging.file.name=/var/log/galacticos/application.log
logging.file.max-size=10MB
logging.file.max-history=10

# PERFORMANCE (OPCIONAL)
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true

# ACTUATOR (PARA MONITOREO)
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=when-authorized
```

---

## 📞 SOPORTE RÁPIDO

Si algo no funciona:

1. **Mira los logs:**
   ```bash
   ssh -i tu-clave.pem ec2-user@3.85.111.48
   sudo tail -f /var/log/galacticos/application.log
   ```

2. **Busca el error en estos archivos:**
   - WOMPI_PRODUCCION.md (sección Troubleshooting)
   - DEPLOYMENT_CHECKLIST_PRODUCCION.md (sección Verificación)
   - ARQUITECTURA_COMPLETA_2024.md (sección Flujos de Seguridad)

3. **Consulta documentación oficial:**
   - Wompi: https://docs.wompi.co
   - Spring Boot: https://spring.io/guides
   - AWS: https://docs.aws.amazon.com

4. **Último recurso - Rollback:**
   ```bash
   bash deploy-produccion.sh
   # Selecciona opción 5 (Rollback)
   ```

---

## ✅ DESPUÉS DE COMPLETAR

Cuando todo funciona:

- [ ] Frontend puede hacer login
- [ ] Frontend puede abrir widget de Wompi
- [ ] Pago se procesa en Wompi
- [ ] Webhook se recibe en backend
- [ ] Estado se actualiza en BD
- [ ] Usuario ve confirmación

Si todo está ✅, **¡FELICIDADES!** 🎉

Tu sistema Wompi está en PRODUCCIÓN.

---

## 🚀 PRÓXIMOS PASOS OPCIONALES

Después de que todo funciona perfecto:

1. **Habilitar HTTPS/SSL:**
   - Ejecutar: `sudo certbot certonly --standalone -d tu-dominio.com`
   - Configurar Nginx para SSL
   - Actualizar CloudFront para HTTPS

2. **Monitoreo y alertas:**
   - CloudWatch para logs centralizados
   - SNS para alertas por email
   - Dashboard de métricas en CloudWatch

3. **Optimización:**
   - CDN para assets estáticos
   - Caching en CloudFront
   - Database read replicas

4. **Seguridad:**
   - AWS WAF en CloudFront
   - Rate limiting
   - Encryption en RDS

---

**¡Ahora tienes Wompi en producción!** 💳✨

Si necesitas ayuda: Revisa los archivos .md de documentación (todos están en la raíz del proyecto)

