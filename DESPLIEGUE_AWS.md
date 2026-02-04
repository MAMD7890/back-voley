# 🚀 Guía Completa: Despliegue del Backend Galacticos en AWS

## 📋 Tabla de Contenidos
1. [Preparación Previa](#preparación-previa)
2. [Configuración de Servicios AWS](#configuración-de-servicios-aws)
3. [Preparar la Aplicación](#preparar-la-aplicación)
4. [Despliegue en EC2](#despliegue-en-ec2)
5. [Despliegue con RDS (Recomendado)](#despliegue-con-rds-recomendado)
6. [Configuración de Dominio y SSL](#configuración-de-dominio-y-ssl)
7. [Monitoreo y Mantenimiento](#monitoreo-y-mantenimiento)
8. [Solución de Problemas](#solución-de-problemas)

---

## Preparación Previa

### Requisitos
- Cuenta AWS activa
- AWS CLI instalado y configurado
- Git instalado
- Maven 3.8+
- Java 17+
- Un archivo `.pem` (clave privada) para acceder a las instancias EC2

### Recomendaciones
- Presupuesto AWS configurado
- Acceso a AWS Console (https://console.aws.amazon.com)
- Nombre de dominio registrado (opcional pero recomendado)

---

## Configuración de Servicios AWS

### Opción 1: Despliegue Simple (EC2 + RDS)

#### Paso 1: Crear una Base de Datos RDS MySQL

1. **Ir a AWS Console → RDS → Crear Base de Datos**

```
Configuración:
- Motor: MySQL 8.0
- Versión: 8.0.35 o superior
- Clase DB: db.t3.micro (capa gratuita)
- Almacenamiento: 20 GB
- Nombre de BD: escuela_voleibol
- Usuario: admin
- Contraseña: (generar segura, mínimo 8 caracteres)
```

2. **Conectividad**
   - VPC: default
   - Acceso público: Sí (para desarrollo)
   - Grupo de seguridad: Crear nuevo

3. **Grupo de Seguridad RDS**
   - Inbound Rule: MySQL/Aurora (3306)
   - Source: 0.0.0.0/0 (CAMBIAR A IP ESPECÍFICA EN PRODUCCIÓN)

4. **Esperar a que la BD esté disponible**
   - Guardar el endpoint (ej: `db-instance.xxxxx.us-east-1.rds.amazonaws.com`)
   - Guardar puerto (por defecto 3306)

---

#### Paso 2: Crear Instancia EC2

1. **Ir a AWS Console → EC2 → Lanzar Instancias**

```
Configuración:
- Nombre: galacticos-backend
- Sistema Operativo: Ubuntu Server 24.04 LTS (free tier)
- Tipo de Instancia: t3.micro o t3.small
- Par de claves: Crear nuevo o usar existente
- Almacenamiento: 30 GB (SSD)
```

2. **Grupo de Seguridad EC2**
   ```
   Inbound Rules:
   - SSH (22) → Source: Tu IP
   - HTTP (80) → Source: 0.0.0.0/0
   - HTTPS (443) → Source: 0.0.0.0/0
   - Custom TCP 8080 → Source: 0.0.0.0/0 (para Spring Boot)
   ```

3. **Elastic IP (Recomendado)**
   - Asignar una IP Elástica para mantener dirección consistente

4. **Guardar:**
   - IP Pública de la instancia
   - DNS público
   - Archivo `.pem` de la clave

---

### Opción 2: Despliegue con Elastic Beanstalk (Más Simple)

#### Paso 1: Preparar Archivo JAR

```bash
# En tu máquina local
mvn clean package
# Archivo generado: target/galacticos-0.0.1-SNAPSHOT.jar
```

#### Paso 2: Crear Aplicación en Elastic Beanstalk

1. **Ir a AWS Console → Elastic Beanstalk → Crear Aplicación**

```
Configuración:
- Nombre: galacticos-app
- Plataforma: Java
- Versión de Java: Java 17 running on 64bit Amazon Linux 2
- Cargar código: Seleccionar JAR compilado
```

2. **Crear Environment**
   - Tipo: Web server environment
   - Instancia: t3.micro
   - Capacidad: Single instance

3. **Configurar Base de Datos RDS**
   - En Elastic Beanstalk → Environment → RDS
   - Usar los datos de la BD RDS creada anteriormente

---

## Preparar la Aplicación

### Paso 1: Crear archivo `application-prod.properties`

Crear archivo: `src/main/resources/application-prod.properties`

```properties
spring.application.name=galacticos

# ========================
# CONFIGURACIÓN MYSQL - PRODUCCIÓN
# ========================
spring.datasource.url=jdbc:mysql://<RDS-ENDPOINT>:3306/escuela_voleibol?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=admin
spring.datasource.password=<TU_CONTRASEÑA_RDS>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ========================
# CONFIGURACIÓN JPA/HIBERNATE
# ========================
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# ========================
# LOGGING
# ========================
logging.level.root=INFO
logging.level.galacticos_app_back=INFO

# ========================
# JWT CONFIGURATION
# ========================
jwt.secret=<CAMBIAR_A_CLAVE_SEGURA_Y_LARGA>
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# ========================
# FILE UPLOAD
# ========================
file.upload-dir=/var/app/uploads
file.max-size=5MB
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

# ========================
# WOMPI PAYMENT GATEWAY - PRODUCCIÓN
# ========================
wompi.public-key=<CAMBIAR_A_CLAVE_PRODUCCIÓN>
wompi.private-key=<CAMBIAR_A_CLAVE_PRODUCCIÓN>
wompi.events-secret=<CAMBIAR_A_SECRETO_PRODUCCIÓN>
wompi.integrity-secret=<CAMBIAR_A_SECRETO_PRODUCCIÓN>
wompi.api-url=https://production.wompi.co/v1
wompi.sandbox=false

# ========================
# CONFIGURACIÓN DEL SERVIDOR
# ========================
server.port=8080
server.servlet.context-path=/api
```

### Paso 2: Compilar el JAR

```bash
# En la raíz del proyecto
mvn clean package -DskipTests

# Archivo generado:
# target/galacticos-0.0.1-SNAPSHOT.jar
```

### Paso 3: Verificar Compilación

```bash
# Comprobar que se generó correctamente
ls -lh target/galacticos-0.0.1-SNAPSHOT.jar
```

---

## Despliegue en EC2

### Opción A: Conexión SSH y Configuración Manual

#### Paso 1: Conectar a la Instancia EC2

```bash
# En PowerShell o Terminal
chmod 400 tu-clave.pem  # Dar permisos a la clave
ssh -i tu-clave.pem ubuntu@<IP-PÚBLICA-EC2>
```

#### Paso 2: Instalar Dependencias en EC2

```bash
# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Java 17
sudo apt install -y openjdk-17-jdk

# Verificar instalación
java -version
# Debe mostrar: openjdk version "17.x.x"

# Instalar MySQL Client (para pruebas)
sudo apt install -y mysql-client

# Crear carpeta para la aplicación
sudo mkdir -p /app/galacticos
sudo chown ubuntu:ubuntu /app/galacticos

# Crear carpeta para uploads
sudo mkdir -p /var/app/uploads
sudo chown ubuntu:ubuntu /var/app/uploads
```

#### Paso 3: Transferir el JAR a EC2

```bash
# Desde tu máquina local (NOT en EC2)
scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar ubuntu@<IP-PÚBLICA-EC2>:/app/galacticos/
```

#### Paso 4: Crear Script de Inicio

En EC2, crear archivo `/app/galacticos/start.sh`:

```bash
#!/bin/bash

# Configurar variables de entorno
export JAVA_OPTS="-Xmx512m -Xms256m"
export SPRING_PROFILES_ACTIVE=prod

# Iniciar aplicación
cd /app/galacticos
java -jar galacticos-0.0.1-SNAPSHOT.jar
```

```bash
# Dar permisos de ejecución
chmod +x /app/galacticos/start.sh
```

#### Paso 5: Crear Servicio Systemd (Para que inicie automáticamente)

```bash
sudo nano /etc/systemd/system/galacticos.service
```

Agregar el siguiente contenido:

```ini
[Unit]
Description=Galacticos Backend Application
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/app/galacticos
ExecStart=/app/galacticos/start.sh
Restart=always
RestartSec=10
Environment="SPRING_PROFILES_ACTIVE=prod"
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# Habilitar y iniciar el servicio
sudo systemctl daemon-reload
sudo systemctl enable galacticos
sudo systemctl start galacticos

# Verificar estado
sudo systemctl status galacticos

# Ver logs en tiempo real
sudo journalctl -u galacticos -f
```

#### Paso 6: Verificar que la Aplicación está Corriendo

```bash
# En la instancia EC2
curl http://localhost:8080/api/health
# Debería retornar: {"status":"UP"}

# O desde tu máquina local
curl http://<IP-PÚBLICA-EC2>:8080/api/health
```

---

### Opción B: Despliegue Automatizado con GitHub Actions

#### Paso 1: Crear Workflow de GitHub Actions

Crear archivo: `.github/workflows/deploy-aws.yml`

```yaml
name: Deploy to AWS EC2

on:
  push:
    branches: [ main, production ]
  workflow_dispatch:

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build with Maven
      run: mvn clean package -DskipTests

    - name: Deploy to AWS EC2
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.AWS_EC2_HOST }}
        username: ubuntu
        key: ${{ secrets.AWS_EC2_KEY }}
        script: |
          cd /app/galacticos
          wget -O galacticos-0.0.1-SNAPSHOT.jar \
            https://github.com/${{ github.repository }}/releases/download/latest/galacticos-0.0.1-SNAPSHOT.jar
          sudo systemctl restart galacticos
          sleep 5
          curl http://localhost:8080/api/health
```

#### Paso 2: Configurar Secretos en GitHub

En tu repositorio:
1. **Settings → Secrets and variables → Actions**
2. Agregar:
   - `AWS_EC2_HOST`: IP pública de EC2
   - `AWS_EC2_KEY`: Contenido del archivo `.pem`

---

## Despliegue con RDS (Recomendado)

### Paso 1: Crear Base de Datos Inicial en RDS

```bash
# Desde tu máquina local
mysql -h <RDS-ENDPOINT> -u admin -p -e "CREATE DATABASE escuela_voleibol;"
```

O importar schema.sql:

```bash
mysql -h <RDS-ENDPOINT> -u admin -p escuela_voleibol < src/main/resources/schema.sql
```

### Paso 2: Verificar Conectividad

```bash
# Desde EC2
mysql -h <RDS-ENDPOINT> -u admin -p -e "SELECT VERSION();"

# Debería mostrar: 8.0.xx
```

### Paso 3: Actualizar application-prod.properties

Reemplazar:
- `<RDS-ENDPOINT>`: El endpoint de RDS (ej: `db-instance.xxxxx.us-east-1.rds.amazonaws.com`)
- `<TU_CONTRASEÑA_RDS>`: La contraseña creada en RDS

```properties
spring.datasource.url=jdbc:mysql://db-instance.xxxxx.us-east-1.rds.amazonaws.com:3306/escuela_voleibol?useSSL=false&serverTimezone=UTC
```

---

## Configuración de Dominio y SSL

### Paso 1: Registrar Dominio

Opciones:
- Route 53 (AWS)
- GoDaddy
- Namecheap
- Google Domains

### Paso 2: Apuntar Dominio a EC2

En el proveedor de dominio:
```
A Record: example.com → <IP-PÚBLICA-EC2>
```

O si usas Elastic IP en AWS:
```
Route 53:
- Type: A
- Value: <ELASTIC-IP>
```

### Paso 3: Instalar Certificado SSL (Let's Encrypt)

```bash
# En la instancia EC2
sudo apt install -y certbot python3-certbot-nginx

# Obtener certificado
sudo certbot certonly --standalone -d example.com

# Certificado guardado en: /etc/letsencrypt/live/example.com/
```

### Paso 4: Configurar Nginx como Reverse Proxy

```bash
sudo apt install -y nginx
```

Crear archivo: `/etc/nginx/sites-available/galacticos`

```nginx
upstream galacticos_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name example.com www.example.com;
    
    # Redirigir a HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name example.com www.example.com;

    # Certificados SSL
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # Configuración SSL segura
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Headers de seguridad
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;

    location / {
        proxy_pass http://galacticos_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Para archivos estáticos
    location /uploads {
        alias /var/app/uploads;
        expires 30d;
    }
}
```

```bash
# Habilitar sitio
sudo ln -s /etc/nginx/sites-available/galacticos /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default

# Probar configuración
sudo nginx -t

# Reiniciar Nginx
sudo systemctl restart nginx

# Verificar certificado SSL (debería ser válido)
curl -I https://example.com
```

### Paso 5: Renovación Automática de Certificados

```bash
# Crear script de renovación
sudo crontab -e

# Agregar línea:
0 3 * * * /usr/bin/certbot renew --quiet && /usr/sbin/service nginx reload
```

---

## Monitoreo y Mantenimiento

### Paso 1: CloudWatch (Monitoreo de Aplicación)

```bash
# Ver logs de la aplicación
sudo journalctl -u galacticos -n 100 -f

# Exportar logs
sudo journalctl -u galacticos > /tmp/galacticos-logs.txt
```

### Paso 2: Crear Alarmas en CloudWatch

1. **AWS Console → CloudWatch → Alarms**
2. Crear alarma para:
   - CPU Utilization > 80%
   - Memory Utilization > 85%
   - Disk Space < 2GB

### Paso 3: Backup de Base de Datos

```bash
# Snapshot automático en RDS
# AWS Console → RDS → Instances → galacticos-db
# Habilitar "Enable automated backups"
# Backup retention period: 7 días (mínimo)

# Backup manual
aws rds create-db-snapshot \
    --db-instance-identifier galacticos-db \
    --db-snapshot-identifier galacticos-backup-$(date +%Y%m%d-%H%M%S)
```

### Paso 4: Monitoreo de Health Check

Crear script: `/app/galacticos/health-check.sh`

```bash
#!/bin/bash

HEALTH_URL="http://localhost:8080/api/health"
LOG_FILE="/var/log/galacticos-health.log"

# Verificar que la aplicación esté respondiendo
if curl -sf $HEALTH_URL > /dev/null 2>&1; then
    echo "[$(date)] Health check OK" >> $LOG_FILE
else
    echo "[$(date)] Health check FAILED - Reiniciando aplicación" >> $LOG_FILE
    sudo systemctl restart galacticos
fi
```

```bash
# Ejecutar cada 5 minutos
sudo crontab -e
# Agregar: */5 * * * * /app/galacticos/health-check.sh
```

---

## Solución de Problemas

### Problema 1: La Aplicación no Inicia

```bash
# Ver logs detallados
sudo journalctl -u galacticos -n 50

# Verificar que el JAR existe
ls -lh /app/galacticos/galacticos-0.0.1-SNAPSHOT.jar

# Probar ejecución manual
cd /app/galacticos
java -jar galacticos-0.0.1-SNAPSHOT.jar
```

### Problema 2: No se Conecta a la Base de Datos

```bash
# Verificar conectividad a RDS
mysql -h <RDS-ENDPOINT> -u admin -p -e "SELECT 1;"

# Verificar grupo de seguridad RDS
# AWS Console → RDS → Instances → galacticos-db → VPC Security Groups
# Debe tener Inbound rule para puerto 3306 desde la IP de EC2

# Ver errores de conexión en logs
sudo journalctl -u galacticos | grep -i "connection"
```

### Problema 3: Puerto 8080 en Uso

```bash
# Encontrar proceso usando el puerto
sudo lsof -i :8080

# Matar proceso
sudo kill -9 <PID>

# O cambiar puerto en application-prod.properties
server.port=9090
```

### Problema 4: Disco Lleno

```bash
# Verificar uso de disco
df -h

# Limpiar logs antiguos
sudo journalctl --vacuum=10d

# Limpiar cache de Java
rm -rf /tmp/*
```

### Problema 5: SSL Certificate Error

```bash
# Verificar certificado
sudo certbot certificates

# Renovar ahora
sudo certbot renew --force-renewal

# Verificar que Nginx está sirviendo el certificado correcto
curl -vI https://example.com
```

---

## Checklist Final de Despliegue

- [ ] Base de datos RDS creada y accesible
- [ ] Instancia EC2 creada y en ejecución
- [ ] Grupos de seguridad configurados correctamente
- [ ] Archivo `application-prod.properties` actualizado
- [ ] JAR compilado y transferido a EC2
- [ ] Servicio Systemd creado e iniciado
- [ ] Aplicación responde en `http://<IP-EC2>:8080/api/health`
- [ ] Dominio apuntando a EC2
- [ ] Certificado SSL instalado
- [ ] Nginx configurado y redirigiendo HTTPS
- [ ] Backups de BD configurados
- [ ] Alarmas de CloudWatch creadas
- [ ] Health check script funcionando
- [ ] Logs siendo guardados correctamente

---

## Costos Estimados (Mes)

```
AWS:
- EC2 t3.micro: $7-10 USD
- RDS MySQL db.t3.micro: $20-30 USD
- Elastic IP (si está sin usar): Gratis
- CloudWatch: Gratis (10 alarmas gratis)
- Data Transfer: Depende del uso

Total Estimado: $30-50 USD/mes (para desarrollo)
```

---

## Referencias

- [AWS EC2 Documentation](https://docs.aws.amazon.com/ec2/)
- [AWS RDS Documentation](https://docs.aws.amazon.com/rds/)
- [Spring Boot AWS Guide](https://docs.spring.io/spring-cloud-aws/docs/current/reference/html/)
- [Let's Encrypt Certbot](https://certbot.eff.org/)
- [Nginx Reverse Proxy](https://nginx.org/en/docs/)

---

**Versión:** 1.0  
**Última actualización:** Febrero 2026  
**Autor:** Equipo de Desarrollo Galacticos
