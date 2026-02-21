# 🧪 GUÍA DE TESTING - PANEL DE PLANES Y CONFIGURACIÓN

**Fecha**: 19 de Febrero de 2026  
**Estado**: Aplicación ejecutándose en puerto 8080

---

## 🚀 INICIO RÁPIDO

### 1. Verificar que la aplicación está corriendo
```bash
curl http://localhost:8080/api/planes
```

Debería retornar: `[]` (lista vacía)

---

## 🔐 OBTENER TOKEN JWT

### Paso 1: Registrarse como Administrador
```bash
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "nombre": "Admin",
  "email": "admin@galacticos.com",
  "password": "Admin123!@#",
  "fotoUrl": null,
  "fotoNombre": null
}
```

**Response**:
```json
{
  "idUsuario": 1,
  "nombre": "Admin",
  "email": "admin@galacticos.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "mensaje": "Usuario registrado exitosamente"
}
```

### Paso 2: Obtener token (si ya existe usuario)
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@galacticos.com",
  "password": "Admin123!@#"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "idUsuario": 1,
  "nombre": "Admin",
  "email": "admin@galacticos.com"
}
```

**Guardar el token para los siguientes tests**:
```
TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📋 TEST 1: CREAR CONFIGURACIÓN DE MATRÍCULA

### Request
```bash
POST http://localhost:8080/api/configuracion
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Precio de matrícula para nuevos estudiantes",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}
```

### Expected Response (201 Created)
```json
{
  "idConfiguracion": 1,
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Precio de matrícula para nuevos estudiantes",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}
```

---

## 📋 TEST 2: CREAR PLAN 1 MES

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "nombre": "Plan 1 mes",
  "descripcion": "Acceso completo a entrenamientos",
  "duracionMeses": 1,
  "precio": 80000,
  "descripcionCorta": "1 mes de clases",
  "activo": true,
  "masPopular": false,
  "ordenVisualizacion": 0
}
```

### Expected Response (201 Created)
```json
{
  "idPlan": 1,
  "nombre": "Plan 1 mes",
  "descripcion": "Acceso completo a entrenamientos",
  "duracionMeses": 1,
  "precio": 80000,
  "precioMensual": 80000,
  "descripcionCorta": "1 mes de clases",
  "activo": true,
  "masPopular": false,
  "ordenVisualizacion": 0
}
```

---

## 📋 TEST 3: CREAR PLAN 2 MESES (POPULAR)

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "nombre": "Plan 2 meses",
  "descripcion": "Acceso completo a entrenamientos",
  "duracionMeses": 2,
  "precio": 150000,
  "descripcionCorta": "2 meses de clases",
  "activo": true,
  "masPopular": true,
  "ordenVisualizacion": 1
}
```

### Expected Response (201 Created)
```json
{
  "idPlan": 2,
  "nombre": "Plan 2 meses",
  "duracionMeses": 2,
  "precio": 150000,
  "precioMensual": 75000,
  "masPopular": true,
  "ordenVisualizacion": 1,
  ...
}
```

**Nota**: `precioMensual` debe ser 75000 (150000 / 2)

---

## 📋 TEST 4: CREAR PLAN 3 MESES

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "nombre": "Plan 3 meses",
  "descripcion": "Acceso completo a entrenamientos",
  "duracionMeses": 3,
  "precio": 210000,
  "descripcionCorta": "3 meses de clases",
  "activo": true,
  "masPopular": false,
  "ordenVisualizacion": 2
}
```

### Expected Response (201 Created)
```json
{
  "idPlan": 3,
  "nombre": "Plan 3 meses",
  "duracionMeses": 3,
  "precio": 210000,
  "precioMensual": 70000,
  "ordenVisualizacion": 2,
  ...
}
```

---

## 📋 TEST 5: OBTENER PLANES ACTIVOS (PÚBLICO)

### Request
```bash
GET http://localhost:8080/api/planes
```

### Expected Response (200 OK)
```json
[
  {
    "idPlan": 1,
    "nombre": "Plan 1 mes",
    "precio": 80000,
    "precioMensual": 80000,
    "masPopular": false,
    "ordenVisualizacion": 0,
    "activo": true
  },
  {
    "idPlan": 2,
    "nombre": "Plan 2 meses",
    "precio": 150000,
    "precioMensual": 75000,
    "masPopular": true,
    "ordenVisualizacion": 1,
    "activo": true
  },
  {
    "idPlan": 3,
    "nombre": "Plan 3 meses",
    "precio": 210000,
    "precioMensual": 70000,
    "masPopular": false,
    "ordenVisualizacion": 2,
    "activo": true
  }
]
```

**Nota**: Deben estar ordenados por `ordenVisualizacion`

---

## 📋 TEST 6: OBTENER PRECIO DE MATRÍCULA (PÚBLICO)

### Request
```bash
GET http://localhost:8080/api/configuracion/precio/matricula
```

### Expected Response (200 OK)
```json
{
  "clave": "PRECIO_MATRICULA",
  "valor": 170000,
  "tipo": "BIGDECIMAL"
}
```

---

## 📋 TEST 7: ACTUALIZAR PRECIO DE UN PLAN

### Request
```bash
PUT http://localhost:8080/api/planes/1
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "precio": 85000
}
```

### Expected Response (200 OK)
```json
{
  "idPlan": 1,
  "nombre": "Plan 1 mes",
  "precio": 85000,
  "precioMensual": 85000,
  ...
}
```

---

## 📋 TEST 8: ACTUALIZAR PRECIO DE MATRÍCULA

### Request
```bash
PATCH http://localhost:8080/api/configuracion/precio/matricula
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "precio": "175000"
}
```

### Expected Response (200 OK)
```json
{
  "idConfiguracion": 1,
  "clave": "PRECIO_MATRICULA",
  "valor": "175000",
  "tipo": "BIGDECIMAL"
}
```

---

## 📋 TEST 9: DESACTIVAR UN PLAN

### Request
```bash
PATCH http://localhost:8080/api/planes/1/desactivar
Authorization: Bearer {TOKEN}
```

### Expected Response (200 OK)
```json
{
  "idPlan": 1,
  "nombre": "Plan 1 mes",
  "activo": false,
  ...
}
```

### Verificar que se desactivó
```bash
GET http://localhost:8080/api/planes
```

Debería retornar solo 2 planes (Plan 1 debe estar fuera)

---

## 📋 TEST 10: OBTENER TODOS LOS PLANES (INCLUYENDO INACTIVOS) - ADMIN

### Request
```bash
GET http://localhost:8080/api/planes/admin/todos
Authorization: Bearer {TOKEN}
```

### Expected Response (200 OK)
```json
[
  {
    "idPlan": 1,
    "nombre": "Plan 1 mes",
    "activo": false,  // Desactivado
    ...
  },
  {
    "idPlan": 2,
    "nombre": "Plan 2 meses",
    "activo": true,
    ...
  },
  {
    "idPlan": 3,
    "nombre": "Plan 3 meses",
    "activo": true,
    ...
  }
]
```

---

## ❌ TEST 11: ERROR - CREAR PLAN SIN NOMBRE

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "duracionMeses": 1,
  "precio": 80000
}
```

### Expected Response (400 Bad Request)
```json
{
  "error": "El nombre del plan es requerido"
}
```

---

## ❌ TEST 12: ERROR - CREAR PLAN CON NOMBRE DUPLICADO

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "nombre": "Plan 2 meses",
  "duracionMeses": 2,
  "precio": 150000
}
```

### Expected Response (400 Bad Request)
```json
{
  "error": "Ya existe un plan con ese nombre"
}
```

---

## ❌ TEST 13: ERROR - CREAR PLAN CON PRECIO NEGATIVO

### Request
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "nombre": "Plan Inválido",
  "duracionMeses": 1,
  "precio": -50000
}
```

### Expected Response (400 Bad Request)
```json
{
  "error": "El precio debe ser mayor a 0"
}
```

---

## ❌ TEST 14: ERROR - SIN AUTENTICACIÓN

### Request
```bash
POST http://localhost:8080/api/planes
Content-Type: application/json

{
  "nombre": "Plan Test",
  "duracionMeses": 1,
  "precio": 80000
}
```

### Expected Response (401 Unauthorized)
```json
{
  "error": "No autorizado"
}
```

---

## ❌ TEST 15: ERROR - ACCESO DENEGADO (NO ADMIN)

### Paso 1: Registrar usuario no-admin
```bash
POST http://localhost:8080/api/auth/register
{
  "nombre": "Usuario Normal",
  "email": "user@galacticos.com",
  "password": "User123!@#"
}
```

### Paso 2: Intentar crear plan con usuario no-admin
```bash
POST http://localhost:8080/api/planes
Authorization: Bearer {TOKEN_USER}
Content-Type: application/json

{
  "nombre": "Plan Unauthorized",
  "duracionMeses": 1,
  "precio": 80000
}
```

### Expected Response (403 Forbidden)
```json
{
  "error": "Acceso denegado"
}
```

---

## 📊 RESUMEN DE TESTS

| # | Test | Status | Nota |
|----|------|--------|------|
| 1 | Crear configuración matrícula | ✅ | Requiere TOKEN |
| 2 | Crear Plan 1 mes | ✅ | Requiere TOKEN |
| 3 | Crear Plan 2 meses | ✅ | Requiere TOKEN |
| 4 | Crear Plan 3 meses | ✅ | Requiere TOKEN |
| 5 | Obtener planes (público) | ✅ | Sin TOKEN |
| 6 | Obtener precio matrícula | ✅ | Sin TOKEN |
| 7 | Actualizar precio plan | ✅ | Requiere TOKEN |
| 8 | Actualizar precio matrícula | ✅ | Requiere TOKEN |
| 9 | Desactivar plan | ✅ | Requiere TOKEN |
| 10 | Obtener todos planes | ✅ | Requiere TOKEN ADMIN |
| 11 | Error sin nombre | ✅ | Validación |
| 12 | Error nombre duplicado | ✅ | Validación |
| 13 | Error precio negativo | ✅ | Validación |
| 14 | Error sin autenticación | ✅ | Seguridad |
| 15 | Error acceso denegado | ✅ | Seguridad |

---

## 🔍 VERIFICACIONES ADICIONALES

### 1. Verificar cálculo automático de precioMensual
```bash
POST /api/planes
{
  "nombre": "Test Calculo",
  "duracionMeses": 3,
  "precio": 120000
}
```

Esperado: `precioMensual = 40000` (120000 / 3)

### 2. Verificar ordenamiento de planes
```bash
GET /api/planes
```

Deben estar ordenados por `ordenVisualizacion` (0, 1, 2, ...)

### 3. Verificar badge "Más Popular"
```bash
GET /api/planes
```

Solo el Plan 2 debe tener `masPopular: true`

### 4. Verificar soft delete
```bash
# Desactivar Plan 1
PATCH /api/planes/1/desactivar

# Obtener planes activos (debe excluir Plan 1)
GET /api/planes

# Obtener todos (debe incluir Plan 1)
GET /api/planes/admin/todos
```

---

## 📱 TESTING CON POSTMAN

### Importar colección
1. Abrir Postman
2. File → Import
3. Seleccionar archivo `.json` con las requests
4. Ejecutar en orden

### Variables de Postman
```
{{BASE_URL}} = http://localhost:8080
{{TOKEN}} = [Token obtenido]
```

---

## 🎯 CONCLUSIÓN

Si todos los tests pasan correctamente:
- ✅ Backend funcionando correctamente
- ✅ Seguridad implementada
- ✅ Validaciones activas
- ✅ Cálculos automáticos correctos
- ✅ Listo para integración con frontend

---

**Testing Guide**: Panel de Planes  
**Versión**: 1.0  
**Fecha**: 19 de Febrero de 2026
