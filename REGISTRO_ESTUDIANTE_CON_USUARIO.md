# Funcionalidad de Registro Automático de Usuario al Crear Estudiante

## Descripción
Se ha implementado una funcionalidad que permite crear automáticamente un Usuario al registrar un Estudiante, cumpliendo con los siguientes requisitos:

## Características Implementadas

### 1. Creación Automática de Usuario
- Al registrar un estudiante mediante el endpoint `/api/estudiantes/registro`, se crea automáticamente un usuario asociado
- El email del usuario será el `correoEstudiante` del estudiante
- El nombre del usuario será el `nombreCompleto` del estudiante
- La contraseña del usuario será el `numeroDocumento` del estudiante (encriptada automáticamente)
- El usuario se crea con rol `STUDENT`

### 2. Validaciones Implementadas
- Valida que el email no esté ya registrado como usuario
- Valida que los campos requeridos no estén vacíos:
  - `correoEstudiante`
  - `nombreCompleto` 
  - `numeroDocumento`

### 3. Respuesta con Tokens
- El método retorna un `AuthResponse` que incluye:
  - `accessToken`: Token de acceso JWT
  - `refreshToken`: Token de renovación
  - `tokenType`: Tipo de token ("Bearer")
  - `expiresIn`: Tiempo de expiración
  - `user`: Información del usuario creado (id, nombre, email, rol)

### 4. Arquitectura Mantenida
- No se duplicó la lógica existente en `AuthService`
- Se reutiliza el método `registerStudent` de `AuthService`
- Se mantiene el estilo y arquitectura actual del proyecto

## Endpoints Disponibles

### Endpoint Original (Mantiene funcionalidad anterior)
```
POST /api/estudiantes
```
- Crea solo el estudiante sin usuario asociado
- Retorna el objeto `Estudiante` creado

### Nuevo Endpoint para Registro Completo
```
POST /api/estudiantes/registro
```
- Crea el estudiante Y el usuario automáticamente
- Retorna `AuthResponse` con tokens de autenticación

## Ejemplo de Uso

### Request
```json
POST /api/estudiantes/registro
Content-Type: application/json

{
  "nombreCompleto": "Juan Pérez García",
  "correoEstudiante": "juan.perez@email.com",
  "numeroDocumento": "12345678",
  "tipoDocumento": "CEDULA_CIUDADANIA",
  "fechaNacimiento": "2010-03-15",
  "edad": 14,
  "sexo": "MASCULINO",
  "direccionResidencia": "Calle 123 #45-67",
  "barrio": "Centro",
  "celularEstudiante": "3001234567",
  "sede": {
    "idSede": 1
  }
}
```

### Response (Exitosa)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 15,
    "nombre": "Juan Pérez García",
    "email": "juan.perez@email.com",
    "fotoUrl": null,
    "rol": "STUDENT"
  }
}
```

### Response (Error - Email Duplicado)
```json
"Ya existe un usuario registrado con el email: juan.perez@email.com"
```

## Manejo de Errores

### Errores de Validación
- Campo faltante: `"El correo del estudiante es obligatorio"`
- Email duplicado: `"Ya existe un usuario registrado con el email: [email]"`
- Rol no encontrado: `"Rol STUDENT no encontrado. Asegúrate de que la base de datos esté inicializada."`

### Status Codes
- `201 CREATED`: Estudiante y usuario creados exitosamente
- `400 BAD REQUEST`: Error de validación o datos faltantes

## Consideraciones Técnicas

1. **Transaccionalidad**: El método `crearConUsuario` está marcado como `@Transactional` para asegurar la consistencia de datos.

2. **Foto de Perfil**: Los usuarios creados automáticamente no tendrán foto de perfil inicial (se puede agregar posteriormente).

3. **Encriptación**: La contraseña (número de documento) se encripta automáticamente usando `PasswordEncoder`.

4. **Rol por Defecto**: Se asigna automáticamente el rol `STUDENT` sin necesidad de especificarlo.

## Archivos Modificados

1. `EstudianteService.java`:
   - Agregado método `crearConUsuario()`
   - Inyección de `AuthService` y `UsuarioRepository`

2. `EstudianteController.java`:
   - Agregado endpoint `/registro`
   - Validaciones de campos requeridos
   - Manejo de errores con respuestas apropiadas