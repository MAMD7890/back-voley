# Guía de Despliegue en AWS EC2

## 1. Preparación del JAR

```bash
# Compilar el proyecto
mvnw clean package -DskipTests

# El JAR estará en: target/galacticos-0.0.1-SNAPSHOT.jar
```

## 2. Configurar EC2 en AWS

### Requisitos:
- **Instancia EC2**: t2.medium o superior
- **SO**: Amazon Linux 2 o Ubuntu 20.04+
- **Puertos abiertos**: 8080 (HTTP), 443 (HTTPS)
- **Security Group**: Permitir tráfico en puertos 8080 y 443

### Instalación en EC2:

```bash
# Actualizar sistema
sudo yum update -y  # Amazon Linux
# o
sudo apt update && sudo apt upgrade -y  # Ubuntu

# Instalar Java 17
sudo yum install java-17-amazon-corretto -y
# o
sudo apt install openjdk-17-jdk -y

# Verificar instalación
java -version

# Crear usuario para la aplicación
sudo useradd -m -s /bin/bash springapp

# Crear directorio de la aplicación
sudo mkdir -p /opt/galacticos
sudo chown springapp:springapp /opt/galacticos
```

## 3. Subir y Configurar la Aplicación

```bash
# Transferir el JAR a EC2
scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar ec2-user@3.85.111.48:/opt/galacticos/

# Conectar a EC2
ssh -i tu-clave.pem ec2-user@3.85.111.48

# Cambiar propietario
sudo chown springapp:springapp /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar

# Crear archivo application-prod.properties
sudo nano /opt/galacticos/application-prod.properties
```

### Contenido de `application-prod.properties`:
```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# Database (ajusta según tu configuración)
spring.datasource.url=jdbc:mysql://tu-rds-endpoint:3306/galacticos_db
spring.datasource.username=admin
spring.datasource.password=TU_PASSWORD
spring.jpa.hibernate.ddl-auto=validate

# JWT Configuration
jwt.secret=tu-secret-key-aqui
jwt.expiration=86400000

# Twilio Configuration
twilio.account-sid=tu-account-sid
twilio.auth-token=tu-auth-token
twilio.phone-number=tu-numero

# Wompi Configuration
wompi.private-key=tu-private-key
wompi.public-key=tu-public-key
wompi.event-key=tu-event-key

# File Upload
file.upload-dir=/opt/galacticos/uploads

# Logging
logging.level.root=INFO
logging.level.galacticos_app_back=DEBUG
```

## 4. Crear servicio systemd para autoencendido

```bash
sudo nano /etc/systemd/system/galacticos.service
```

### Contenido del archivo:
```ini
[Unit]
Description=Galacticos Application
After=network.target

[Service]
Type=simple
User=springapp
Group=springapp
WorkingDirectory=/opt/galacticos

ExecStart=/usr/bin/java \
  -jar galacticos-0.0.1-SNAPSHOT.jar \
  --spring.config.location=file:application-prod.properties \
  --server.address=0.0.0.0 \
  --server.port=8080

Restart=always
RestartSec=10

# Logs
StandardOutput=journal
StandardError=journal
SyslogIdentifier=galacticos

[Install]
WantedBy=multi-user.target
```

### Ejecutar el servicio:
```bash
sudo systemctl daemon-reload
sudo systemctl enable galacticos.service
sudo systemctl start galacticos.service

# Verificar estado
sudo systemctl status galacticos.service

# Ver logs
sudo journalctl -u galacticos.service -f
```

## 5. Configurar Nginx como Reverse Proxy (Opcional pero Recomendado)

```bash
# Instalar Nginx
sudo yum install nginx -y  # Amazon Linux
# o
sudo apt install nginx -y  # Ubuntu

# Crear configuración
sudo nano /etc/nginx/conf.d/galacticos.conf
```

### Contenido de `galacticos.conf`:
```nginx
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
        
        # CORS headers
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, X-Requested-With' always;
        
        if ($request_method = 'OPTIONS') {
            return 204;
        }
    }

    # Servir archivos estáticos
    location /uploads/ {
        alias /opt/galacticos/uploads/;
    }
}
```

### Activar Nginx:
```bash
sudo systemctl enable nginx
sudo systemctl start nginx
sudo systemctl reload nginx
```

## 6. Configurar SSL con Let's Encrypt (Recomendado)

```bash
# Instalar Certbot
sudo yum install certbot python3-certbot-nginx -y  # Amazon Linux
# o
sudo apt install certbot python3-certbot-nginx -y  # Ubuntu

# Obtener certificado (si tienes dominio real)
sudo certbot --nginx -d 3-85-111-48.nip.io

# Renovación automática
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

## 7. Pruebas de Conectividad

```bash
# Test de login (sin autenticación)
curl -X POST http://3-85-111-48.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password123"}'

# Test de registro (sin autenticación)
curl -X POST http://3-85-111-48.nip.io/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","email":"juan@example.com","password":"pass123"}'
```

## 8. Troubleshooting

### Ver logs en tiempo real:
```bash
sudo journalctl -u galacticos.service -f
```

### Reiniciar aplicación:
```bash
sudo systemctl restart galacticos.service
```

### Verificar puertos:
```bash
sudo netstat -tulpn | grep 8080
sudo netstat -tulpn | grep 80
```

### Ver variables de entorno:
```bash
sudo systemctl show galacticos.service
```

## 9. Monitoreo Recomendado

Instalar CloudWatch Agent en EC2 para monitoreo desde AWS Console:

```bash
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm
```

---

**Nota**: Reemplaza los valores de configuración (JWT secret, BD, etc.) con tus valores reales en producción.
