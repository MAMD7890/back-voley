# Comandos Exactos - Copiar y Pegar

## 📋 Paso 1: Compilar Localmente

```bash
cd c:\Users\Admin\Documents\GitHub\back-voley
mvnw clean package -DskipTests
```

**Resultado esperado:** ✅ `BUILD SUCCESS`

---

## 📋 Paso 2: Preparar Credenciales

```bash
# En tu máquina, ubicar la clave PEM
cd ~
ls -la | grep .pem
# Debe mostrar algo como: galacticos-key.pem

# Si no la tienes, descargar de AWS Console:
# EC2 → Instances → Tu instancia → Security → Key pair → Download PEM
```

---

## 📋 Paso 3: Transferir JAR a EC2

### Opción A: Desde Windows (PowerShell)

```powershell
$keyPath = "$env:USERPROFILE\galacticos-key.pem"
$jarPath = "C:\Users\Admin\Documents\GitHub\back-voley\target\galacticos-0.0.1-SNAPSHOT.jar"
$ec2IP = "3.85.111.48"

# Hacer permiso SSH en Windows primero
icacls $keyPath /inheritance:r /grant:r "$env:USERNAME`:F"

# Transferir
scp -i $keyPath $jarPath "ec2-user@${ec2IP}:/tmp/"
```

### Opción B: Desde Linux/Mac

```bash
scp -i ~/galacticos-key.pem \
    target/galacticos-0.0.1-SNAPSHOT.jar \
    ec2-user@3.85.111.48:/tmp/
```

---

## 📋 Paso 4: Conectar a EC2 y Configurar

```bash
ssh -i ~/galacticos-key.pem ec2-user@3.85.111.48
```

Una vez dentro de EC2, ejecutar estos comandos:

### 4.1 Instalar Java 17

```bash
sudo yum update -y
sudo yum install -y java-17-amazon-corretto
java -version
```

### 4.2 Crear estructura de directorios

```bash
sudo mkdir -p /opt/galacticos/uploads
sudo useradd -m -s /bin/bash springapp 2>/dev/null || true
sudo mv /tmp/galacticos-0.0.1-SNAPSHOT.jar /opt/galacticos/
sudo chown springapp:springapp /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
```

### 4.3 Crear archivo de propiedades

```bash
sudo tee /opt/galacticos/application-prod.properties > /dev/null << 'EOF'
server.port=8080
server.servlet.context-path=/
spring.datasource.url=jdbc:mysql://localhost:3306/galacticos_db
spring.datasource.username=galacticos_user
spring.datasource.password=CambiaEsPassword123!
spring.jpa.hibernate.ddl-auto=validate
jwt.secret=TuSecretMuySeguroQueDebeSerMayorA32Caracteres123456789
jwt.expiration=86400000
logging.level.root=INFO
logging.level.galacticos_app_back=DEBUG
file.upload-dir=/opt/galacticos/uploads
EOF
```

### 4.4 Crear servicio systemd

```bash
sudo tee /etc/systemd/system/galacticos.service > /dev/null << 'EOF'
[Unit]
Description=Galacticos Volleyball Application
After=network.target

[Service]
Type=simple
User=springapp
Group=springapp
WorkingDirectory=/opt/galacticos
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -jar galacticos-0.0.1-SNAPSHOT.jar --spring.config.location=file:/opt/galacticos/application-prod.properties --server.address=0.0.0.0
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=galacticos

[Install]
WantedBy=multi-user.target
EOF
```

### 4.5 Iniciar el servicio

```bash
sudo systemctl daemon-reload
sudo systemctl enable galacticos.service
sudo systemctl start galacticos.service
sleep 5
sudo systemctl status galacticos.service
```

---

## 📋 Paso 5: Verificar que Funciona

### En EC2, test local:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

**Respuesta esperada:**
```json
{"success":false,"message":"Usuario o contraseña incorrectos"}
```
✅ Si ves esta respuesta (sin 401), ¡funciona!

### Desde tu máquina, test remoto:

```bash
curl -X POST http://3.85.111.48:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

**Respuesta esperada:** Mismo JSON que arriba (sin 401)

---

## 📋 Paso 6: Configurar Nginx (Opcional pero Recomendado)

En EC2:

```bash
sudo yum install -y nginx

sudo tee /etc/nginx/conf.d/galacticos.conf > /dev/null << 'EOF'
upstream galacticos {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name 3-85-111-48.nip.io;

    location / {
        proxy_pass http://galacticos;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
    }
}
EOF

sudo systemctl enable nginx
sudo systemctl start nginx
sudo systemctl reload nginx
```

---

## 📋 Paso 7: Test Final desde tu Máquina

### Test 1: Login sin token

```bash
curl -X POST http://3-85-111-48.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```

**Respuesta esperada (200 OK):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {...}
}
```

### Test 2: Crear estudiante

```bash
curl -X POST http://3-85-111-48.nip.io/api/estudiantes \
  -H "Content-Type: application/json" \
  -d '{
    "nombreCompleto": "Juan Pérez García",
    "tipoDocumento": "CC",
    "numeroDocumento": "1023456789",
    "fechaNacimiento": "2008-05-15",
    "edad": 16,
    "sexo": "MASCULINO",
    "correoEstudiante": "juan@example.com",
    "sede": {"idSede": 1},
    "nivelActual": "INICIANTE"
  }'
```

---

## 🔍 Troubleshooting Rápido

### Si NO funciona (aún ves 401):

```bash
# En EC2:
ssh -i ~/galacticos-key.pem ec2-user@3.85.111.48

# Ver logs:
sudo journalctl -u galacticos.service -n 50 -f

# Si ves error de base de datos:
sudo systemctl restart galacticos.service
sleep 5
sudo systemctl status galacticos.service

# Si aún no funciona:
# 1. Verificar que el JAR está ahí:
ls -lh /opt/galacticos/

# 2. Verificar que el puerto 8080 está escuchando:
sudo netstat -tulpn | grep 8080

# 3. Verificar Security Group en AWS Console:
# EC2 → Security Groups → Inbound Rules
# Debe permitir puerto 8080 desde 0.0.0.0/0
```

---

## 📊 Ver Estado en Tiempo Real

```bash
ssh -i ~/galacticos-key.pem ec2-user@3.85.111.48

# Ver logs:
sudo journalctl -u galacticos.service -f

# Ver recursos:
watch -n 1 'free -h && echo "---" && df -h'

# En otra terminal, ver tráfico:
sudo tail -f /var/log/nginx/galacticos_access.log
```

---

## ✅ Checklist Final

- [ ] JAR compilado localmente
- [ ] JAR transferido a EC2
- [ ] Java 17 instalado en EC2
- [ ] Directorio `/opt/galacticos` creado
- [ ] Archivo `application-prod.properties` creado
- [ ] Servicio systemd creado e iniciado
- [ ] `curl localhost:8080/api/auth/login` retorna sin 401
- [ ] `curl 3.85.111.48:8080/api/auth/login` retorna sin 401
- [ ] `curl 3-85-111-48.nip.io/api/auth/login` retorna sin 401
- [ ] Nginx configurado (opcional)

**Si todos checkboxes están ✅, ¡estás listo en producción!**

---

## 🆘 Última Opción: Restart Todo

Si nada funciona:

```bash
ssh -i ~/galacticos-key.pem ec2-user@3.85.111.48

# Detener servicio:
sudo systemctl stop galacticos.service

# Reiniciar:
sudo systemctl start galacticos.service
sleep 10

# Ver logs:
sudo journalctl -u galacticos.service -n 100

# Si sigue con error, revisar propiedades:
cat /opt/galacticos/application-prod.properties

# Si la BD no está accesible:
mysql -h localhost -u galacticos_user -p -D galacticos_db -e "SELECT 1"
```

---

¡Cualquier duda, revisa los archivos de documentación! 📚
