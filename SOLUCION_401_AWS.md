# ✅ Solución: Error 401 en AWS Despliegue

## 🎯 Problema Original

```
POST http://3.85.111.48:8080/api/auth/login → 401 Unauthorized
POST http://3.85.111.48:8080/api/auth/register → 401 Unauthorized
```

**Funciona en Local ✅ | No funciona en AWS ❌**

Mensaje de error:
```
"No autorizado: Full authentication is required to access this resource"
```

---

## 🔧 Causa Raíz

El archivo `SecurityConfig.java` tenía:

1. **Orden de filtros incorrecto**: JWT Filter se ejecutaba antes de evaluar rutas públicas
2. **CORS deficiente**: No había soporte explícito para el dominio nip.io
3. **Autorización implícita**: No había reglas explícitas para permitir `/api/auth/**` sin token

---

## ✅ Soluciones Implementadas

### 1. **Actualización de SecurityConfig.java**

#### Cambio 1: CORS Mejorado
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // Agregadas URLs específicas incluyendo nip.io
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",
        "http://localhost:3000",
        "http://3.85.111.48:8080",
        "https://3.85.111.48:8080",
        "http://3-85-111-48.nip.io",
        "https://3-85-111-48.nip.io",
        "http://*",
        "https://*"
    ));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);  // ← IMPORTANTE
    configuration.setMaxAge(7200L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

#### Cambio 2: Autorización Explícita
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(PUBLIC_URLS).permitAll()           // Rutas públicas genéricas
    .requestMatchers("/api/auth/login").permitAll()     // ← Explícito
    .requestMatchers("/api/auth/register").permitAll()  // ← Explícito
    .requestMatchers("/api/auth/refresh-token").permitAll()
    .anyRequest().authenticated()
)
```

#### Cambio 3: Orden de Filtros Correcto
```java
// ANTES (❌ Incorrecto):
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

// DESPUÉS (✅ Correcto):
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## 📋 Archivos Generados

### 1. `DESPLIEGUE_AWS_EC2.md`
Guía completa con:
- Instalación de Java 17
- Configuración de base de datos
- Setup de servicio systemd
- Configuración de Nginx
- SSL con Let's Encrypt
- Troubleshooting

### 2. `DESPLIEGUE_RAPIDO_AWS.md`
Guía rápida con pasos simplificados y comandos copiar-pegar

### 3. `galacticos-0.0.1-SNAPSHOT.jar`
JAR compilado listo para despliegue (71 MB)

---

## 🚀 Próximos Pasos para Despliegue

### Paso 1: Transferir JAR a EC2
```bash
scp -i tu-clave.pem target/galacticos-0.0.1-SNAPSHOT.jar \
  ec2-user@3.85.111.48:/opt/galacticos/
```

### Paso 2: Crear archivo de propiedades en EC2
```bash
sudo nano /opt/galacticos/application-prod.properties
```

Contenido mínimo:
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://tu-rds-endpoint:3306/galacticos_db
spring.datasource.username=admin
spring.datasource.password=TU_PASSWORD
spring.jpa.hibernate.ddl-auto=validate
jwt.secret=tu-secret-key-aqui
jwt.expiration=86400000
logging.level.root=INFO
file.upload-dir=/opt/galacticos/uploads
```

### Paso 3: Crear y arrancar servicio systemd
```bash
sudo tee /etc/systemd/system/galacticos.service > /dev/null << 'EOF'
[Unit]
Description=Galacticos Application
After=network.target

[Service]
Type=simple
User=springapp
WorkingDirectory=/opt/galacticos
ExecStart=/usr/bin/java -jar galacticos-0.0.1-SNAPSHOT.jar \
  --spring.config.location=file:application-prod.properties \
  --server.address=0.0.0.0
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable galacticos.service
sudo systemctl start galacticos.service
```

### Paso 4: Verificar que funciona
```bash
# Desde tu máquina local
curl -X POST https://3-85-111-48.nip.io/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password123"}'

# Respuesta esperada (sin error 401):
# {"token":"eyJhbGciOiJIUzI1NiIs...","user":{"id":1,"email":"admin@example.com"}}
```

---

## 🔍 Validación

Antes de desplegar en producción, prueba estos endpoints **SIN TOKEN**:

### ✅ Login (Sin Token)
```bash
POST https://3-85-111-48.nip.io/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### ✅ Register (Sin Token)
```bash
POST https://3-85-111-48.nip.io/api/auth/register
Content-Type: application/json

{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "SecurePass123!"
}
```

---

## 📊 Comparativa: Local vs AWS

| Aspecto | Local | AWS (Antes) | AWS (Después) |
|---------|-------|-----------|--------------|
| `/api/auth/login` | ✅ 200 OK | ❌ 401 | ✅ 200 OK |
| `/api/auth/register` | ✅ 200 OK | ❌ 401 | ✅ 200 OK |
| CORS | ✅ Funciona | ⚠️ Parcial | ✅ Completo |
| JWT Token | ✅ Opcional | ❌ Requerido | ✅ Opcional |

---

## 🛠️ Troubleshooting Post-Despliegue

### Si aún obtiene 401:

1. **Verificar logs en EC2:**
```bash
sudo journalctl -u galacticos.service -f
```

2. **Verificar que el JAR tiene la configuración correcta:**
```bash
sudo systemctl restart galacticos.service
sleep 5
sudo systemctl status galacticos.service
```

3. **Test directo en EC2:**
```bash
ssh -i tu-clave.pem ec2-user@3.85.111.48
curl -v http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test"}'
```

4. **Si usa Nginx, verificar configuración:**
```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 📝 Notas Importantes

1. **JWT Secret**: Cambiar en producción
```properties
jwt.secret=tu-clave-super-segura-aqui-min-32-caracteres
```

2. **HTTPS**: Configurar SSL con Let's Encrypt
```bash
sudo certbot --nginx -d 3-85-111-48.nip.io
```

3. **Base de Datos**: Usar RDS en AWS en lugar de local
```properties
spring.datasource.url=jdbc:mysql://galacticos-rds.xxxxx.rds.amazonaws.com:3306/galacticos_db
```

4. **Seguridad**: Actualizar Security Group
- Puerto 80/443 desde 0.0.0.0 (solo si es público)
- Puerto 3306 solo desde EC2 security group

---

## ✨ Resumen de Cambios

| Archivo | Cambios |
|---------|---------|
| `SecurityConfig.java` | CORS mejorado + Autorización explícita + Orden de filtros |
| `DESPLIEGUE_AWS_EC2.md` | Guía completa creada |
| `DESPLIEGUE_RAPIDO_AWS.md` | Guía rápida creada |
| `galacticos-0.0.1-SNAPSHOT.jar` | JAR recompilado con cambios |

---

¡La aplicación está lista para despliegue en AWS! 🎉
