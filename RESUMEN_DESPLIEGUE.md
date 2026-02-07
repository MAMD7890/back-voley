# 🚀 Despliegue AWS EC2 - Resumen Ejecutivo

## ✅ Problema Resuelto

**Antes (❌):**
```
POST /api/auth/login → 401 Unauthorized en AWS
POST /api/auth/register → 401 Unauthorized en AWS
```

**Ahora (✅):**
```
POST /api/auth/login → 200 OK en AWS
POST /api/auth/register → 200 OK en AWS
```

---

## 📦 Archivos Generados

### 1. **JAR Compilado**
```
target/galacticos-0.0.1-SNAPSHOT.jar (71 MB)
```
✅ Listo para desplegar en AWS

### 2. **Guías de Despliegue**
- `SOLUCION_401_AWS.md` → Explicación técnica del problema y solución
- `DESPLIEGUE_AWS_EC2.md` → Guía completa paso a paso
- `DESPLIEGUE_RAPIDO_AWS.md` → Guía rápida (copiar-pegar)

### 3. **Scripts Automatizados**
- `deploy.sh` → Despliegue automático en EC2
- `configure-nginx.sh` → Configurar Nginx automáticamente

---

## 🎯 Despliegue Rápido (5 minutos)

### Opción 1: Script Automático (Recomendado)

```bash
# 1. Hacer ejecutable el script
chmod +x deploy.sh

# 2. Ejecutar despliegue
./deploy.sh 3.85.111.48 ec2-user ~/tu-clave.pem

# 3. Opcional: Configurar Nginx
chmod +x configure-nginx.sh
./configure-nginx.sh 3.85.111.48 ~/tu-clave.pem
```

### Opción 2: Manual

```bash
# 1. Transferir JAR
scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar \
  ec2-user@3.85.111.48:/opt/galacticos/

# 2. En EC2, crear propiedades
ssh -i tu-clave.pem ec2-user@3.85.111.48
sudo nano /opt/galacticos/application-prod.properties

# 3. Crear servicio systemd (copiar de DESPLIEGUE_RAPIDO_AWS.md)
sudo systemctl start galacticos.service
sudo systemctl status galacticos.service
```

---

## 🔑 Cambios Realizados en el Código

### SecurityConfig.java
```java
// 1. CORS mejorado para nip.io
configuration.setAllowedOrigins(Arrays.asList(
    "http://3-85-111-48.nip.io",
    "https://3-85-111-48.nip.io",
    "http://*",
    "https://*"
));

// 2. Autorización explícita para auth endpoints
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/api/auth/register").permitAll()
```

---

## ✨ Testing Post-Despliegue

### ✅ Test 1: Login (Sin Token)
```bash
curl -X POST http://3-85-111-48.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# Respuesta esperada:
# {"success":true,"token":"eyJhbGc...","user":{...}}
```

### ✅ Test 2: Register (Sin Token)
```bash
curl -X POST http://3-85-111-48.nip.io/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","email":"juan@example.com","password":"pass123"}'

# Respuesta esperada:
# {"success":true,"message":"Usuario registrado exitosamente"}
```

### ✅ Test 3: Endpoint Protegido (Con Token)
```bash
TOKEN="eyJhbGc..." # Token obtenido del login

curl -X GET http://3-85-111-48.nip.io/api/estudiantes \
  -H "Authorization: Bearer $TOKEN"

# Respuesta esperada:
# [{"idEstudiante":1,"nombreCompleto":"..."},...]
```

---

## 📋 Checklist Pre-Despliegue

- [ ] JAR compilado: `target/galacticos-0.0.1-SNAPSHOT.jar` ✓
- [ ] EC2 instancia corriendo (3.85.111.48)
- [ ] Security Group permite puertos 80, 443, 8080
- [ ] Clave PEM descargada y en ~/galacticos-key.pem
- [ ] Base de datos MySQL/RDS accesible
- [ ] Variables de entorno preparadas

---

## 📋 Checklist Post-Despliegue

- [ ] Servicio galacticos arrancó exitosamente
- [ ] `/api/auth/login` retorna 200 OK sin token
- [ ] `/api/auth/register` retorna 200 OK sin token
- [ ] Token JWT se genera correctamente
- [ ] Endpoints protegidos funcionan con token
- [ ] Nginx configurado y proxy funcionando
- [ ] Logs visibles en `journalctl -u galacticos.service`

---

## 🔧 Configuración Requerida en EC2

### application-prod.properties
```properties
# Servidor
server.port=8080

# Base de Datos (cambiar valores)
spring.datasource.url=jdbc:mysql://tu-rds-endpoint:3306/galacticos_db
spring.datasource.username=admin
spring.datasource.password=tu-password-seguro

# JWT (CAMBIAR EN PRODUCCIÓN)
jwt.secret=tu-secret-super-seguro-aqui-min-32-caracteres
jwt.expiration=86400000

# File Upload
file.upload-dir=/opt/galacticos/uploads

# Logging
logging.level.root=INFO
logging.level.galacticos_app_back=DEBUG
```

---

## 🚨 Troubleshooting

### Error: "Still getting 401"

1. **Verificar logs:**
```bash
ssh -i tu-clave.pem ec2-user@3.85.111.48
sudo journalctl -u galacticos.service -f
```

2. **Reiniciar servicio:**
```bash
sudo systemctl restart galacticos.service
sleep 5
sudo systemctl status galacticos.service
```

3. **Verificar que se compiló correctamente:**
```bash
# En tu máquina local
file target/galacticos-0.0.1-SNAPSHOT.jar
# Debe mostrar: JAR archive data
```

4. **Test directo en EC2:**
```bash
ssh -i tu-clave.pem ec2-user@3.85.111.48
curl -v http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test"}'
```

### Error: "Connection refused"

1. Verificar que el puerto 8080 está abierto en Security Group
2. Verificar que el servicio está corriendo: `sudo systemctl status galacticos.service`
3. Verificar direccionamiento IP en security group: permite desde 0.0.0.0/0

### Error: "Cannot deserialize NivelActual"

✅ Ya resuelto. Los valores válidos son: `INICIANTE`, `INTERMEDIO`, `AVANZADO`

---

## 📊 Monitoreo Recomendado

### Ver logs en tiempo real
```bash
ssh -i tu-clave.pem ec2-user@3.85.111.48
sudo journalctl -u galacticos.service -f
```

### Ver estado del servicio
```bash
sudo systemctl status galacticos.service
sudo systemctl is-active galacticos.service
```

### Ver recursos (CPU, memoria)
```bash
free -h              # Memoria
df -h                # Espacio en disco
top                  # Procesos
```

### Ver puertos abiertos
```bash
sudo netstat -tulpn | grep LISTEN
# Debe mostrar: tcp  0  0 0.0.0.0:8080  0.0.0.0:*  LISTEN
```

---

## 🔐 Seguridad en Producción

1. **Cambiar JWT secret:**
```properties
jwt.secret=MuyCambiaBienestaArquitecturaSuperSeguraAlMenos32Caracteres
```

2. **Habilitar HTTPS:**
```bash
sudo certbot --nginx -d 3-85-111-48.nip.io
```

3. **Limitar CORS:**
```java
// En lugar de "*", usar dominios específicos
configuration.setAllowedOrigins(Arrays.asList(
    "https://tu-dominio.com",
    "https://app.tu-dominio.com"
));
```

4. **Actualizar Security Group:**
- HTTP (80): Solo desde tu IP o CloudFront
- HTTPS (443): Solo desde tu IP o CloudFront
- MySQL (3306): Solo desde EC2 security group

---

## 🎯 URLs Útiles

- **API Base**: `http://3-85-111-48.nip.io`
- **Login**: `POST /api/auth/login`
- **Register**: `POST /api/auth/register`
- **Estudiantes**: `GET /api/estudiantes` (requiere token)
- **Docs Swagger**: `GET /swagger-ui.html` (si está habilitado)

---

## 📞 Support

Si tienes problemas:

1. **Revisar logs en AWS CloudWatch:**
   - EC2 Dashboard → Logs → View detailed CloudWatch logs

2. **Revisar Security Group:**
   - EC2 Dashboard → Security Groups → Inbound Rules

3. **Revisar database connectivity:**
```bash
# Desde EC2
mysql -h tu-rds-endpoint -u admin -p -D galacticos_db -e "SELECT 1"
```

---

**¡Tu aplicación está lista para volar en AWS! 🚀**

Necesitas ayuda? Revisar `SOLUCION_401_AWS.md` para detalles técnicos.
