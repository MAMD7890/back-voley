# REGISTRO Y LOGIN DE ESTUDIANTES - GUÍA DE PRUEBAS

## ✅ CAMBIOS IMPLEMENTADOS

1. **Rol STUDENT creado automáticamente** al iniciar la aplicación
2. **Nuevo endpoint de registro específico para estudiantes** que asigna automáticamente el rol STUDENT
3. **El login ahora devuelve correctamente el rol** en la respuesta

---

## 🔹 ENDPOINT 1: Registro de Estudiante

**URL:** `POST http://localhost:8081/api/auth/register-student`

**Content-Type:** `multipart/form-data`

### Parámetros (form-data en Postman):

| Key | Type | Value | Requerido |
|-----|------|-------|-----------|
| nombre | Text | Juan Estudiante | ✅ Sí |
| email | Text | estudiante@example.com | ✅ Sí |
| password | Text | Pass123456 | ✅ Sí (mínimo 6 caracteres) |
| foto | File | [Seleccionar imagen] | ✅ Sí |

### Pasos en Postman:
1. Crear nueva request **POST**
2. URL: `http://localhost:8081/api/auth/register-student`
3. Tab **Body** → **form-data**
4. Agregar los 4 campos como se muestra arriba
5. Para `foto`, cambiar tipo a **File** y seleccionar una imagen
6. Click en **Send**

### Respuesta esperada:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "nombre": "Juan Estudiante",
    "email": "estudiante@example.com",
    "fotoUrl": "http://localhost:8081/uploads/profiles/...",
    "rol": "STUDENT"  ← ¡AQUÍ ESTÁ EL ROL!
  }
}
```

---

## 🔹 ENDPOINT 2: Login de Estudiante

**URL:** `POST http://localhost:8081/api/auth/login`

**Content-Type:** `application/json`

### Body (JSON):
```json
{
  "email": "estudiante@example.com",
  "password": "Pass123456"
}
```

### Pasos en Postman:
1. Crear nueva request **POST**
2. URL: `http://localhost:8081/api/auth/login`
3. Tab **Headers** → Add `Content-Type: application/json`
4. Tab **Body** → **raw** → **JSON**
5. Pegar el JSON de arriba
6. Click en **Send**

### Respuesta esperada:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "nombre": "Juan Estudiante",
    "email": "estudiante@example.com",
    "fotoUrl": "http://localhost:8081/uploads/profiles/...",
    "rol": "STUDENT"  ← ¡AQUÍ ESTÁ EL ROL!
  }
}
```

---

## 🔹 ENDPOINT 3: Obtener Usuario Actual

**URL:** `GET http://localhost:8081/api/auth/me`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Respuesta esperada:
```json
{
  "id": 1,
  "nombre": "Juan Estudiante",
  "email": "estudiante@example.com",
  "fotoUrl": "http://localhost:8081/uploads/profiles/...",
  "rol": "STUDENT"  ← ¡AQUÍ ESTÁ EL ROL!
}
```

---

## 🔹 ENDPOINT 4: Registro Normal (con rol personalizado)

**URL:** `POST http://localhost:8081/api/auth/register`

**Content-Type:** `multipart/form-data`

Si necesitas registrar con un rol específico (ADMIN, USER, PROFESOR):

| Key | Type | Value |
|-----|------|-------|
| nombre | Text | Admin User |
| email | Text | admin@example.com |
| password | Text | Admin123 |
| idRol | Text | 1 (ADMIN), 2 (USER), 3 (PROFESOR) |
| foto | File | [Imagen] |

---

## 🛠️ SOLUCIÓN DE PROBLEMAS

### ❌ Problema: El login devuelve `rol: null`

**Causa:** El usuario fue creado antes de que el rol STUDENT existiera.

**Solución:**
1. Reinicia la aplicación (para que DataInitializer cree el rol STUDENT)
2. Ejecuta en MySQL:
```sql
-- Ver los roles disponibles
SELECT * FROM rol;

-- Si STUDENT no existe, créalo
INSERT INTO rol (nombre) VALUES ('STUDENT');

-- Actualizar usuarios existentes para asignarles el rol STUDENT
UPDATE usuario SET id_rol = (SELECT id_rol FROM rol WHERE nombre = 'STUDENT' LIMIT 1) 
WHERE email = 'estudiante@example.com';
```

### ❌ Problema: Error "Rol STUDENT no encontrado"

**Solución:** Reinicia la aplicación. El componente `DataInitializer` creará automáticamente todos los roles al iniciar.

---

## 📊 ROLES DISPONIBLES

| ID | Nombre | Descripción |
|----|--------|-------------|
| 1 | ADMIN | Administrador del sistema |
| 2 | USER | Usuario genérico |
| 3 | PROFESOR | Profesor/entrenador |
| 4 | STUDENT | Estudiante |

---

## 🎯 FLUJO COMPLETO DE REGISTRO DE ESTUDIANTE

1. **Registro:** POST `/api/auth/register-student` 
   - Se crea el usuario con rol STUDENT automáticamente
   - Devuelve accessToken y rol: "STUDENT"

2. **El frontend guarda el token y el rol**

3. **Login posterior:** POST `/api/auth/login`
   - Valida credenciales
   - Devuelve nuevo accessToken y rol: "STUDENT"

4. **Protección de rutas en frontend:**
   - El guard verifica que rol === "STUDENT"
   - Permite acceso solo a rutas de estudiante

---

## ✨ VENTAJAS

✅ **No se requiere ID de rol** - Se asigna automáticamente
✅ **El login siempre devuelve el rol** - Sincronizado con la BD
✅ **Guards del frontend funcionan** - Reciben rol: "STUDENT"
✅ **Validación automática** - No permite registrar sin rol
