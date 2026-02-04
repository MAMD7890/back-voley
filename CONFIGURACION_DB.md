# Configuración Base de Datos - Escuela de Voleibol

## Pasos de Configuración

### 1. Requisitos Previos
- MySQL 5.7 o superior instalado y ejecutándose
- Java 17 o superior
- Maven 3.8 o superior

### 2. Crear Base de Datos en MySQL
Ejecuta el siguiente comando en MySQL antes de iniciar la aplicación:

```sql
CREATE DATABASE IF NOT EXISTS escuela_voleibol;
```

O si prefieres usar la línea de comandos:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS escuela_voleibol;"
```

### 3. Configurar Conexión a Base de Datos
Edita el archivo `src/main/resources/application.properties`:

```properties
# URL de conexión MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/escuela_voleibol?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true

# Usuario MySQL (por defecto root)
spring.datasource.username=root

# Contraseña MySQL (dejar vacío si no hay contraseña)
spring.datasource.password=

# Controlador JDBC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 4. Configuración Automática de Hibernate
El proyecto está configurado para que Hibernate genere automáticamente las tablas al iniciar:

```properties
# Opción: create-drop (elimina y crea tablas en cada inicio - ideal para desarrollo)
spring.jpa.hibernate.ddl-auto=create-drop

# Otras opciones disponibles:
# - create: crea las tablas (error si ya existen)
# - update: actualiza tablas existentes
# - validate: valida que existan (no modifica)
# - none: no hace nada
```

### 5. Iniciar la Aplicación
```bash
# Con Maven
mvn spring-boot:run

# O compilar y ejecutar
mvn clean install
java -jar target/galacticos-0.0.1-SNAPSHOT.jar
```

## Estructura de Entidades Creadas

### Gestión de Usuarios
- `Rol` - Roles de usuarios
- `Usuario` - Usuarios del sistema

### Información Base
- `Sede` - Sedes de la escuela
- `Categoria` - Categorías de equipos (edad, nivel)
- `Profesor` - Profesores/entrenadores

### Estructura Deportiva
- `Equipo` - Equipos de voleibol
- `ProfesorEquipo` - Relación N:M entre profesores y equipos

### Estudiantes y Membresías
- `Estudiante` - Información completa de estudiantes (datos personales, académicos, médicos, etc.)
- `Membresia` - Membresías de estudiantes en equipos
- `AsistenciaEstudiante` - Registro de asistencia de estudiantes

### Profesores
- `AsistenciaProfesor` - Registro de horas trabajadas por profesores

### Gestión Financiera
- `Pago` - Pagos de membresías
- `RecordatorioPago` - Recordatorios de pago por WhatsApp
- `Gasto` - Gastos de la escuela

## Variables de Entorno Alternativas

Si deseas usar variables de entorno en lugar de properties:

```bash
# Linux/Mac
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/escuela_voleibol"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD=""

# Windows
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/escuela_voleibol
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=
```

## Solución de Problemas

### Error de conexión MySQL
- Verifica que MySQL esté ejecutándose
- Comprueba el usuario y contraseña
- Asegúrate que la base de datos existe
- Intenta conectar manualmente: `mysql -u root -p`

### Error de controlador JDBC
- Verifica que `mysql-connector-java` esté en pom.xml
- Ejecuta: `mvn clean install` para descargar dependencias

### Tablas no se crean
- Verifica que `spring.jpa.hibernate.ddl-auto=create-drop` está en properties
- Revisa los logs para errores de entidad
- Asegúrate que todas las entidades tienen `@Entity`

## Notas Importantes

1. El proyecto usa **Lombok** - asegúrate de tener la extensión instalada en tu IDE
2. Las entidades están en el paquete `galacticos_app_back.galacticos.entity`
3. Se usa **Jakarta EE** (jakarta.persistence) en lugar de javax.persistence (Spring Boot 3.x)
4. El script SQL original está disponible para referencia
