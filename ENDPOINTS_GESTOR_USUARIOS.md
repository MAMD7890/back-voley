# 📋 Endpoints Gestor de Usuarios

**Base URL:** `http://localhost:8080`  
**Header requerido:** `Authorization: Bearer <JWT_TOKEN>`  
**Protección:** `@PreAuthorize("hasRole('ADMIN')")`

---

## GET - Obtener todos los usuarios

**Endpoint:**
```
GET /api/usuarios
```

**Response (200 OK):**
```json
[
  {
    "idUsuario": 1,
    "nombre": "Admin User",
    "email": "admin@example.com",
    "tipoDocumento": "CC",
    "numeroDocumento": "123456789",
    "telefono": "3001234567",
    "estado": true,
    "rol": {
      "idRol": 1,
      "nombre": "ADMIN"
    },
    "username": null,
    "requiereChangioPassword": false,
    "fotoUrl": "https://example.com/avatar.jpg",
    "fotoNombre": "avatar.jpg"
  },
  {
    "idUsuario": 2,
    "nombre": "Profesor Principal",
    "email": "profesor@example.com",
    "tipoDocumento": "CC",
    "numeroDocumento": "987654321",
    "telefono": "3009876543",
    "estado": true,
    "rol": {
      "idRol": 3,
      "nombre": "PROFESOR"
    },
    "username": null,
    "requiereChangioPassword": false,
    "fotoUrl": null,
    "fotoNombre": null
  }
]
```

---

## GET - Obtener usuario por ID

**Endpoint:**
```
GET /api/usuarios/{id}
```

**Ejemplo:**
```
GET /api/usuarios/1
```

**Response (200 OK):**
```json
{
  "idUsuario": 1,
  "nombre": "Admin User",
  "email": "admin@example.com",
  "tipoDocumento": "CC",
  "numeroDocumento": "123456789",
  "telefono": "3001234567",
  "estado": true,
  "rol": {
    "idRol": 1,
    "nombre": "ADMIN"
  },
  "username": null,
  "requiereChangioPassword": false,
  "fotoUrl": "https://example.com/avatar.jpg",
  "fotoNombre": "avatar.jpg"
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found"
}
```

---

## GET - Obtener usuario por email

**Endpoint:**
```
GET /api/usuarios/email/{email}
```

**Ejemplo:**
```
GET /api/usuarios/email/admin@example.com
```

**Response (200 OK):**
```json
{
  "idUsuario": 1,
  "nombre": "Admin User",
  "email": "admin@example.com",
  "tipoDocumento": "CC",
  "numeroDocumento": "123456789",
  "telefono": "3001234567",
  "estado": true,
  "rol": {
    "idRol": 1,
    "nombre": "ADMIN"
  },
  "username": null,
  "requiereChangioPassword": false,
  "fotoUrl": "https://example.com/avatar.jpg",
  "fotoNombre": "avatar.jpg"
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found"
}
```

---

## POST - Crear nuevo usuario

**Endpoint:**
```
POST /api/usuarios
Content-Type: application/json
```

**Request Body:**
```json
{
  "nombre": "Nuevo Usuario",
  "email": "nuevo@example.com",
  "password": "PasswordSegura123!",
  "tipoDocumento": "CC",
  "numeroDocumento": "555555555",
  "telefono": "3007777777",
  "estado": true,
  "rol": {
    "idRol": 3
  },
  "fotoUrl": "https://example.com/new-avatar.jpg",
  "fotoNombre": "new-avatar.jpg"
}
```

**Response (200 OK):**
```json
{
  "idUsuario": 5,
  "nombre": "Nuevo Usuario",
  "email": "nuevo@example.com",
  "tipoDocumento": "CC",
  "numeroDocumento": "555555555",
  "telefono": "3007777777",
  "estado": true,
  "rol": {
    "idRol": 3,
    "nombre": "PROFESOR"
  },
  "username": null,
  "requiereChangioPassword": false,
  "fotoUrl": "https://example.com/new-avatar.jpg",
  "fotoNombre": "new-avatar.jpg"
}
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "El email ya está registrado"
}
```

---

## PUT - Actualizar usuario

**Endpoint:**
```
PUT /api/usuarios/{id}
Content-Type: application/json
```

**Ejemplo:**
```
PUT /api/usuarios/5
```

**Request Body:**
```json
{
  "nombre": "Usuario Actualizado",
  "email": "actualizado@example.com",
  "password": "NuevaPassword123!",
  "tipoDocumento": "CC",
  "numeroDocumento": "111111111",
  "telefono": "3005555555",
  "estado": true,
  "rol": {
    "idRol": 2
  },
  "fotoUrl": "https://example.com/updated-avatar.jpg",
  "fotoNombre": "updated-avatar.jpg"
}
```

**Response (200 OK):**
```json
{
  "idUsuario": 5,
  "nombre": "Usuario Actualizado",
  "email": "actualizado@example.com",
  "tipoDocumento": "CC",
  "numeroDocumento": "111111111",
  "telefono": "3005555555",
  "estado": true,
  "rol": {
    "idRol": 2,
    "nombre": "USER"
  },
  "username": null,
  "requiereChangioPassword": false,
  "fotoUrl": "https://example.com/updated-avatar.jpg",
  "fotoNombre": "updated-avatar.jpg"
}
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found"
}
```

---

## DELETE - Eliminar usuario

**Endpoint:**
```
DELETE /api/usuarios/{id}
```

**Ejemplo:**
```
DELETE /api/usuarios/5
```

**Response (204 No Content):**
```
(sin cuerpo)
```

---

## ⚠️ Respuestas de Error

### 401 Unauthorized (sin JWT)
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### 403 Forbidden (sin rol ADMIN)
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied: user is not allowed to access this resource"
}
```

### 400 Bad Request (datos inválidos)
```json
{
  "timestamp": "2026-04-11T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email ya existe en la base de datos"
}
```

---

## 📝 Notas

- ✅ Todo endpoint requiere **JWT válido** en header `Authorization: Bearer <token>`
- ✅ Solo usuarios con rol **ADMIN** pueden acceder
- ✅ Al crear usuario, la **contraseña se encripta automáticamente** (BCrypt)
- ✅ Email debe ser **único** en la BD
- ✅ Al actualizar, si no envías contraseña se mantiene la anterior
- ✅ Estados posibles del usuario: `true` (activo), `false` (inactivo)

---

## 🔍 IDs de Roles

| idRol | nombre |
|-------|--------|
| 1 | ADMIN |
| 2 | USER |
| 3 | PROFESOR |
| 4 | STUDENT |

---

## 📱 Ejemplo cURL

**Obtener todos:**
```bash
curl -X GET "http://localhost:8080/api/usuarios" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Crear usuario:**
```bash
curl -X POST "http://localhost:8080/api/usuarios" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Nuevo Usuario",
    "email": "nuevo@example.com",
    "password": "PasswordSegura123!",
    "tipoDocumento": "CC",
    "numeroDocumento": "555555555",
    "telefono": "3007777777",
    "estado": true,
    "rol": {"idRol": 3}
  }'
```

**Actualizar usuario:**
```bash
curl -X PUT "http://localhost:8080/api/usuarios/5" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Usuario Actualizado",
    "email": "actualizado@example.com",
    "tipoDocumento": "CC",
    "numeroDocumento": "111111111",
    "telefono": "3005555555",
    "estado": true,
    "rol": {"idRol": 2}
  }'
```

**Eliminar usuario:**
```bash
curl -X DELETE "http://localhost:8080/api/usuarios/5" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```
