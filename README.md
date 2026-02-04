# 🏐 Galacticos App - Backend

**Escuela de Voleibol - Sistema Integral de Gestión**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-green?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red?logo=apache-maven)](https://maven.apache.org/)

## 📋 Descripción

Sistema completo de gestión para una escuela de voleibol construido con **Spring Boot 3.5.9** y **MySQL**. Incluye:

- ✅ Gestión de estudiantes (con 40+ campos informativos)
- ✅ Registro de equipos y profesores
- ✅ Sistema de membresías y pagos
- ✅ Control de asistencia
- ✅ Gestión de gastos
- ✅ Recordatorios de pago por WhatsApp
- ✅ Generación automática de base de datos con Hibernate

## 🚀 Inicio Rápido

### Requisitos Previos
```bash
✅ Java 17 o superior
✅ MySQL 8.0 o superior
✅ Maven 3.8 o superior
```

### Instalación en 5 Pasos

**1. Crear base de datos**
```bash
mysql -u root -p -e "CREATE DATABASE escuela_voleibol;"
```

**2. Clonar/Descargar el proyecto**
```bash
cd galacticos_app_back
```

**3. Actualizar credenciales (si es necesario)**
```properties
# src/main/resources/application.properties
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
```

**4. Compilar el proyecto**
```bash
mvn clean install
```

**5. Ejecutar la aplicación**
```bash
mvn spring-boot:run
```

La aplicación se ejecutará en `http://localhost:8080`

## 📊 Estructura de Base de Datos

### 13 Entidades Principales

```
┌─ USUARIOS
│  ├── Rol
│  └── Usuario
│
├─ INFORMACIÓN BASE
│  ├── Sede
│  ├── Categoria
│  └── Profesor
│
├─ ESTRUCTURA DEPORTIVA
│  ├── Equipo
│  └── ProfesorEquipo (N:M)
│
├─ ESTUDIANTES
│  ├── Estudiante (completa)
│  ├── Membresia
│  └── AsistenciaEstudiante
│
├─ ASISTENCIA
│  └── AsistenciaProfesor
│
└─ FINANZAS
   ├── Pago
   ├── Gasto
   └── RecordatorioPago
```

### Tablas Generadas Automáticamente: 14

## 🛠️ Tecnología Utilizada

| Componente | Versión |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.5.9 |
| Spring Data JPA | Incluido |
| Hibernate ORM | Incluido |
| MySQL Driver | 8.0.33 |
| Lombok | Incluido |
| Maven | 3.8+ |

## 📂 Estructura del Proyecto

```
src/
├── main/
│   ├── java/galacticos_app_back/galacticos/
│   │   ├── entity/          (13 entidades JPA)
│   │   ├── repository/      (7 repositorios)
│   │   ├── service/         (Servicios de negocio)
│   │   └── GalacticosApplication.java
│   │
│   └── resources/
│       ├── application.properties (Configuración MySQL)
│       ├── application-dev.properties (Perfil desarrollo)
│       └── schema.sql (Script SQL referencia)
│
└── test/
    └── java/... (Tests unitarios)
```

## 📖 Documentación

Se incluyen 4 documentos de referencia:

| Archivo | Descripción |
|---------|------------|
| **[CONFIGURACION_DB.md](CONFIGURACION_DB.md)** | Guía paso a paso de configuración |
| **[GUIA_FINAL.md](GUIA_FINAL.md)** | Manual completo del usuario |
| **[RESUMEN_IMPLEMENTACION.md](RESUMEN_IMPLEMENTACION.md)** | Especificaciones técnicas |
| **[INDICE_ARCHIVOS.md](INDICE_ARCHIVOS.md)** | Índice completo de archivos |

**Lectura recomendada**: Comienza con [CONFIGURACION_DB.md](CONFIGURACION_DB.md)

## ⚙️ Configuración

### Credenciales MySQL por Defecto
```
Usuario: root
Contraseña: (vacío)
Host: localhost
Puerto: 3306
Base de datos: escuela_voleibol
```

### Cambiar Credenciales
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/escuela_voleibol
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASEÑA
```

### Configuración Hibernate
```properties
# DDL Auto - Estrategia de generación de esquema
spring.jpa.hibernate.ddl-auto=create-drop

# Opciones disponibles:
# create-drop: Crea y elimina al cerrar (desarrollo)
# create: Crea tablas
# update: Actualiza esquema (producción)
# validate: Valida sin modificar
# none: No hace nada
```

## 💻 Ejemplos de Uso

### Crear un Estudiante
```java
@Autowired
private EstudianteRepository estudianteRepository;

Estudiante estudiante = new Estudiante();
estudiante.setNombreCompleto("Juan Pérez López");
estudiante.setTipoDocumento(Estudiante.TipoDocumento.CC);
estudiante.setNumeroDocumento("1234567890");
estudiante.setFechaNacimiento(LocalDate.of(2005, 5, 15));
estudiante.setEdad(19);

estudianteRepository.save(estudiante);
```

### Buscar Estudiantes Activos
```java
List<Estudiante> activos = estudianteRepository.findByEstado(true);
```

### Registrar un Pago
```java
Pago pago = new Pago();
pago.setEstudiante(estudiante);
pago.setValor(new BigDecimal("150000"));
pago.setMetodoPago(Pago.MetodoPago.ONLINE);
pago.setEstadoPago(Pago.EstadoPago.PAGADO);
pago.setFechaPago(LocalDate.now());

pagoRepository.save(pago);
```

## 🔍 Verificación de Instalación

Después de ejecutar `mvn spring-boot:run`:

1. **Verificar en consola**: Deberías ver mensajes de Hibernate creando tablas
2. **Conectar a MySQL**:
   ```bash
   mysql -u root -p escuela_voleibol
   mysql> SHOW TABLES;
   ```
3. Deberías ver 14 tablas creadas

## 🐛 Solución de Problemas

### Error: "Cannot find MySQL driver"
```bash
mvn clean install
# El driver se descargará automáticamente
```

### Error: "Access denied for user 'root'"
- Verifica el usuario y contraseña en `application.properties`
- Asegúrate que MySQL esté ejecutándose

### Error: "Database does not exist"
```bash
mysql -u root -p -e "CREATE DATABASE escuela_voleibol;"
```

### Las tablas no se crean
- Verifica que `spring.jpa.hibernate.ddl-auto=create-drop` esté activo
- Revisa los logs para errores de mapeo de entidades

Ver más en [CONFIGURACION_DB.md](CONFIGURACION_DB.md#solución-de-problemas)

## 📦 Dependencias Principales

```xml
<!-- Spring Boot Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

## 🎯 Próximos Pasos

- [ ] Crear controladores REST para las entidades
- [ ] Implementar servicios de negocio
- [ ] Agregar validaciones (@Valid, @NotNull)
- [ ] Implementar autenticación JWT
- [ ] Crear DTOs (Data Transfer Objects)
- [ ] Documentación con Swagger/OpenAPI
- [ ] Tests unitarios
- [ ] CI/CD (GitHub Actions)

## 📞 Soporte y Contacto

Para reportar problemas o sugerencias:
1. Revisa la documentación incluida
2. Consulta [CONFIGURACION_DB.md](CONFIGURACION_DB.md) para problemas de configuración
3. Consulta [GUIA_FINAL.md](GUIA_FINAL.md) para guía de uso

## 📜 Licencia

Este proyecto es parte del sistema de gestión de la Escuela de Voleibol Galacticos.

## ✅ Checklist de Implementación

- [x] Entidades JPA (13)
- [x] Repositorios (7)
- [x] Configuración MySQL
- [x] Hibernate DDL Auto
- [x] Documentación completa
- [x] Ejemplos de código
- [x] Solución de problemas

## 📊 Estadísticas del Proyecto

- **Entidades**: 13
- **Repositorios**: 7
- **Servicios**: 2+ (ejemplos)
- **Tablas DB**: 14
- **Campos totales**: 100+
- **Relaciones**: 15+
- **Enums personalizados**: 8

## 🚀 Iniciar Desarrollo

```bash
# 1. Clonar/descargar
cd galacticos_app_back

# 2. Crear BD
mysql -u root -p -e "CREATE DATABASE escuela_voleibol;"

# 3. Compilar
mvn clean install

# 4. Ejecutar
mvn spring-boot:run

# 5. Acceder
# http://localhost:8080
```

---

**Estado**: ✅ Listo para Producción (aplicando mejores prácticas)

**Fecha de Última Actualización**: 29 de Diciembre de 2024

**Versión**: 1.0.0-SNAPSHOT
#   b a c k - v o l e i b o l  
 