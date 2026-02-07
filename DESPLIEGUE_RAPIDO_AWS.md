# Despliegue Rápido en AWS EC2 con URL nip.io

## Problema Resuelto ✅
- Los endpoints `/api/auth/login` y `/api/auth/register` ahora permiten acceso sin token en AWS
- CORS configurado para funcionar con `https://3-85-111-48.nip.io`

## Cambios Realizados en `SecurityConfig.java`

### 1. CORS Mejorado
- Agregadas todas las URLs necesarias (localhost, IP pública, nip.io)
- Headers CORS permitidos: `*`
- Credenciales habilitadas: `true`

### 2. Autorización Explícita
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(PUBLIC_URLS).permitAll()
    .requestMatchers("/api/auth/login").permitAll()
    .requestMatchers("/api/auth/register").permitAll()
    .requestMatchers("/api/auth/refresh-token").permitAll()
    .anyRequest().authenticated()
)
```

### 3. Orden de Filtros Correcta
- JWT Filter se ejecuta **ANTES** de `UsernamePasswordAuthenticationFilter`
- Esto permite que las rutas públicas sean evaluadas primero

---

## Pasos Rápidos para Despliegue

### 1. **Compilar JAR**
```bash
cd c:\Users\Admin\Documents\GitHub\back-voley
.\mvnw.cmd clean package -DskipTests
# JAR generado en: target/galacticos-0.0.1-SNAPSHOT.jar
```

### 2. **Copiar a EC2**
```bash
scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar \
  ec2-user@3.85.111.48:/opt/galacticos/
```

### 3. **En la EC2, crear properties**
```bash
cat > /opt/galacticos/application-prod.properties << EOF
server.port=8080
server.servlet.context-path=/
spring.datasource.url=jdbc:mysql://tu-rds-endpoint:3306/galacticos_db
spring.datasource.username=admin
spring.datasource.password=TU_PASSWORD
spring.jpa.hibernate.ddl-auto=validate
jwt.secret=tu-secret-key-super-seguro
jwt.expiration=86400000
logging.level.root=INFO
file.upload-dir=/opt/galacticos/uploads
EOF
```

### 4. **Crear servicio systemd**
```bash
sudo tee /etc/systemd/system/galacticos.service > /dev/null << EOF
[Unit]
Description=Galacticos Application
After=network.target

[Service]
Type=simple
User=springapp
WorkingDirectory=/opt/galacticos
ExecStart=/usr/bin/java -jar galacticos-0.0.1-SNAPSHOT.jar --spring.config.location=file:application-prod.properties --server.address=0.0.0.0
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable galacticos.service
sudo systemctl start galacticos.service
```

### 5. **Configurar Nginx (opcional pero recomendado)**
```bash
sudo tee /etc/nginx/conf.d/galacticos.conf > /dev/null << EOF
upstream galacticos {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name 3-85-111-48.nip.io;

    location / {
        proxy_pass http://galacticos;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;
        
        if (\$request_method = 'OPTIONS') {
            return 204;
        }
    }
}
EOF

sudo systemctl restart nginx
```

### 6. **Prueba de Conectividad**
```bash
# Test Login (sin token)
curl -X POST https://3-85-111-48.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Test Register (sin token)
curl -X POST https://3-85-111-48.nip.io/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan","email":"juan@example.com","password":"pass123"}'
```

---

## ¿Por qué funcionaba en Local pero no en AWS?

### Problema Original:
En AWS, el **orden de ejecución de filtros** era:
1. ❌ JWT Filter ejecutaba primero
2. ❌ Verificaba token (no existía → error 401)
3. ❌ Luego se evaluaba si la ruta era pública

### Solución Implementada:
1. ✅ CORS se ejecuta primero (pre-flight)
2. ✅ Autorización evalúa rutas públicas EXPLÍCITAMENTE
3. ✅ JWT Filter solo procesa requests autenticados

---

## Variables de Entorno Necesarias en EC2

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://rds-endpoint:3306/galacticos_db
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=tu-password
export JWT_SECRET=tu-secret-muy-seguro
export TWILIO_ACCOUNT_SID=tu-sid
export TWILIO_AUTH_TOKEN=tu-token
export WOMPI_PRIVATE_KEY=tu-key
```

---

## Checklist Final

- [ ] JAR compilado exitosamente
- [ ] Propiedades configuradas en EC2
- [ ] Directorio `/opt/galacticos` creado
- [ ] Servicio systemd creado e iniciado
- [ ] Nginx configurado (si se usa)
- [ ] Security Group permite puertos 80 y 443
- [ ] Base de datos RDS accesible desde EC2
- [ ] Test de login sin token funciona ✅
- [ ] Test de register sin token funciona ✅

---

## Monitoreo Post-Despliegue

```bash
# Ver logs en tiempo real
sudo journalctl -u galacticos.service -f

# Ver estado
sudo systemctl status galacticos.service

# Reiniciar si es necesario
sudo systemctl restart galacticos.service
```

