# 📚 API REST - ESCUELA DE VOLEIBOL

## 🎯 Resumen General

Se ha creado un **CRUD completo** para cada una de las **13 entidades** del sistema con:

- ✅ 13 Servicios (@Service)
- ✅ 13 Repositorios (extends JpaRepository)
- ✅ 13 Controladores REST (@RestController)

---

## 📋 Endpoints Disponibles

### 1. ROLES
**Base URL:** `/api/roles`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los roles |
| GET | `/{id}` | Obtener rol por ID |
| POST | `/` | Crear nuevo rol |
| PUT | `/{id}` | Actualizar rol |
| DELETE | `/{id}` | Eliminar rol |

**Ejemplo POST:**
```json
{
  "nombre": "Administrador"
}
```

---

### 2. USUARIOS
**Base URL:** `/api/usuarios`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los usuarios |
| GET | `/{id}` | Obtener usuario por ID |
| GET | `/email/{email}` | Obtener usuario por email |
| POST | `/` | Crear nuevo usuario |
| PUT | `/{id}` | Actualizar usuario |
| DELETE | `/{id}` | Eliminar usuario |

**Ejemplo POST:**
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "password": "hashedPassword",
  "estado": true,
  "idRol": 1
}
```

---

### 3. SEDES
**Base URL:** `/api/sedes`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todas las sedes |
| GET | `/{id}` | Obtener sede por ID |
| GET | `/activos/lista` | Obtener sedes activas |
| POST | `/` | Crear nueva sede |
| PUT | `/{id}` | Actualizar sede |
| DELETE | `/{id}` | Eliminar sede |
| PATCH | `/{id}/desactivar` | Desactivar sede |

**Ejemplo POST:**
```json
{
  "nombre": "Sede Centro",
  "direccion": "Calle Principal 123",
  "telefono": "3001234567",
  "estado": true
}
```

---

### 4. CATEGORÍAS
**Base URL:** `/api/categorias`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todas las categorías |
| GET | `/{id}` | Obtener categoría por ID |
| POST | `/` | Crear nueva categoría |
| PUT | `/{id}` | Actualizar categoría |
| DELETE | `/{id}` | Eliminar categoría |

**Ejemplo POST:**
```json
{
  "nombre": "U-18",
  "rangoEdad": "14-18 años",
  "descripcion": "Categoría juvenil"
}
```

---

### 5. PROFESORES
**Base URL:** `/api/profesores`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los profesores |
| GET | `/{id}` | Obtener profesor por ID |
| GET | `/activos/lista` | Obtener profesores activos |
| POST | `/` | Crear nuevo profesor |
| PUT | `/{id}` | Actualizar profesor |
| DELETE | `/{id}` | Eliminar profesor |
| PATCH | `/{id}/desactivar` | Desactivar profesor |

**Ejemplo POST:**
```json
{
  "nombre": "Carlos García",
  "documento": "1234567890",
  "telefono": "3001234567",
  "salarioPorClase": 50000.00,
  "fotoUrl": "https://example.com/foto.jpg",
  "fotoNombre": "carlos_garcia.jpg",
  "estado": true
}
```

---

### 6. EQUIPOS
**Base URL:** `/api/equipos`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los equipos |
| GET | `/{id}` | Obtener equipo por ID |
| GET | `/activos/lista` | Obtener equipos activos |
| GET | `/sede/{idSede}` | Obtener equipos por sede |
| GET | `/categoria/{idCategoria}` | Obtener equipos por categoría |
| POST | `/` | Crear nuevo equipo |
| PUT | `/{id}` | Actualizar equipo |
| DELETE | `/{id}` | Eliminar equipo |
| PATCH | `/{id}/desactivar` | Desactivar equipo |

**Ejemplo POST:**
```json
{
  "nombre": "Titanes U-18",
  "idCategoria": 1,
  "idSede": 1,
  "horario": "Lunes y miércoles 18:00",
  "fotoUrl": "https://example.com/team.jpg",
  "estado": true
}
```

---

### 7. PROFESOR-EQUIPO (Relación N:M)
**Base URL:** `/api/profesor-equipo`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todas las asignaciones |
| GET | `/{id}` | Obtener asignación por ID |
| POST | `/` | Crear nueva asignación |
| PUT | `/{id}` | Actualizar asignación |
| DELETE | `/{id}` | Eliminar asignación |

**Ejemplo POST:**
```json
{
  "idProfesor": 1,
  "idEquipo": 1,
  "rol": "PRINCIPAL"
}
```

**Roles disponibles:** PRINCIPAL, ASISTENTE

---

### 8. ESTUDIANTES
**Base URL:** `/api/estudiantes`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los estudiantes |
| GET | `/{id}` | Obtener estudiante por ID |
| GET | `/activos/lista` | Obtener estudiantes activos |
| GET | `/nivel/{nivel}` | Obtener por nivel |
| GET | `/sede/{idSede}` | Obtener por sede |
| POST | `/` | Crear nuevo estudiante |
| PUT | `/{id}` | Actualizar estudiante |
| DELETE | `/{id}` | Eliminar estudiante |
| PATCH | `/{id}/desactivar` | Desactivar estudiante |

**Niveles disponibles:** INICIANTE, INTERMEDIO, AVANZADO

**Ejemplo POST (datos principales):**
```json
{
  "nombreCompleto": "Maria López",
  "tipoDocumento": "CC",
  "numeroDocumento": "1098765432",
  "fechaNacimiento": "2006-05-15",
  "edad": 18,
  "sexo": "FEMENINO",
  "direccionResidencia": "Calle 5 No 10",
  "barrio": "Centro",
  "celularEstudiante": "3101234567",
  "correoEstudiante": "maria@example.com",
  "idSede": 1,
  "nombreTutor": "Pedro López",
  "parentescoTutor": "Padre",
  "eps": "EPS SALUD",
  "tipoSangre": "O+",
  "posicionPreferida": "Libero",
  "dominancia": "DERECHA",
  "nivelActual": "INTERMEDIO",
  "estado": true
}
```

---

### 9. MEMBRESÍAS
**Base URL:** `/api/membresias`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todas las membresías |
| GET | `/{id}` | Obtener membresía por ID |
| GET | `/estudiante/{idEstudiante}` | Obtener membresías de estudiante |
| GET | `/equipo/{idEquipo}` | Obtener membresías de equipo |
| GET | `/vigentes/lista` | Obtener membresías vigentes |
| POST | `/` | Crear nueva membresía |
| PUT | `/{id}` | Actualizar membresía |
| DELETE | `/{id}` | Eliminar membresía |

**Estados:** VIGENTE, VENCIDA

**Ejemplo POST:**
```json
{
  "idEstudiante": 1,
  "idEquipo": 1,
  "fechaInicio": "2025-01-01",
  "fechaFin": "2025-12-31",
  "valorMensual": 150000.00,
  "estado": "VIGENTE"
}
```

---

### 10. PAGOS
**Base URL:** `/api/pagos`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los pagos |
| GET | `/{id}` | Obtener pago por ID |
| GET | `/estudiante/{idEstudiante}` | Obtener pagos de estudiante |
| GET | `/pendientes/lista` | Obtener pagos pendientes |
| GET | `/pagados/lista` | Obtener pagos pagados |
| GET | `/vencidos/lista` | Obtener pagos vencidos |
| POST | `/` | Registrar nuevo pago |
| PUT | `/{id}` | Actualizar pago |
| PATCH | `/{id}/estado/{estado}` | Cambiar estado de pago |
| DELETE | `/{id}` | Eliminar pago |

**Estados:** PAGADO, PENDIENTE, VENCIDO
**Métodos:** ONLINE, EFECTIVO

**Ejemplo POST:**
```json
{
  "idEstudiante": 1,
  "mesPagado": "Enero 2025",
  "valor": 150000.00,
  "metodoPago": "ONLINE",
  "referenciaPago": "TRX123456",
  "fechaPago": "2025-01-10",
  "horaPago": "14:30:00",
  "estadoPago": "PAGADO"
}
```

---

### 11. ASISTENCIA ESTUDIANTES
**Base URL:** `/api/asistencia-estudiante`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los registros |
| GET | `/{id}` | Obtener registro por ID |
| POST | `/` | Crear nuevo registro |
| PUT | `/{id}` | Actualizar registro |
| DELETE | `/{id}` | Eliminar registro |

**Ejemplo POST:**
```json
{
  "idEstudiante": 1,
  "idEquipo": 1,
  "fecha": "2025-01-10",
  "asistio": true,
  "observaciones": "Excelente desempeño"
}
```

---

### 12. ASISTENCIA PROFESORES
**Base URL:** `/api/asistencia-profesor`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los registros |
| GET | `/{id}` | Obtener registro por ID |
| POST | `/` | Crear nuevo registro |
| PUT | `/{id}` | Actualizar registro |
| DELETE | `/{id}` | Eliminar registro |

**Ejemplo POST:**
```json
{
  "idProfesor": 1,
  "idEquipo": 1,
  "fecha": "2025-01-10",
  "horasTrabajadas": 2.0
}
```

---

### 13. GASTOS
**Base URL:** `/api/gastos`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos los gastos |
| GET | `/{id}` | Obtener gasto por ID |
| POST | `/` | Crear nuevo gasto |
| PUT | `/{id}` | Actualizar gasto |
| DELETE | `/{id}` | Eliminar gasto |

**Ejemplo POST:**
```json
{
  "concepto": "Compra de redes",
  "descripcion": "2 redes de voleibol profesional",
  "monto": 500000.00,
  "fecha": "2025-01-10",
  "idSede": 1
}
```

---

### 14. RECORDATORIOS DE PAGO
**Base URL:** `/api/recordatorios-pago`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Obtener todos |
| GET | `/{id}` | Obtener por ID |
| POST | `/` | Crear nuevo |
| PUT | `/{id}` | Actualizar |
| DELETE | `/{id}` | Eliminar |

**Estados:** ENVIADO, PENDIENTE

**Ejemplo POST:**
```json
{
  "idEstudiante": 1,
  "fechaEnvio": "2025-01-15T10:30:00",
  "mensaje": "Recordatorio: Tu cuota vence el 31 de enero",
  "estado": "PENDIENTE"
}
```

---

## 🔐 Headers Comunes

Todos los endpoints aceptan:

```
Content-Type: application/json
Accept: application/json
```

---

## 📝 Códigos HTTP Esperados

| Código | Significado |
|--------|------------|
| 200 | OK - Operación exitosa |
| 201 | Created - Recurso creado |
| 204 | No Content - Operación exitosa sin contenido |
| 400 | Bad Request - Solicitud inválida |
| 404 | Not Found - Recurso no encontrado |
| 500 | Internal Server Error - Error del servidor |

---

## 🔄 Flujos de Ejemplo

### Ejemplo 1: Crear un Estudiante
```bash
POST /api/estudiantes
Content-Type: application/json

{
  "nombreCompleto": "Juan García",
  "tipoDocumento": "CC",
  "numeroDocumento": "1234567890",
  "fechaNacimiento": "2005-03-15",
  "edad": 19,
  "idSede": 1,
  "estado": true
}

Response: 200 OK
{
  "idEstudiante": 1,
  "nombreCompleto": "Juan García",
  ...
}
```

### Ejemplo 2: Crear una Membresía
```bash
POST /api/membresias
Content-Type: application/json

{
  "idEstudiante": 1,
  "idEquipo": 1,
  "fechaInicio": "2025-01-01",
  "fechaFin": "2025-12-31",
  "valorMensual": 150000.00,
  "estado": "VIGENTE"
}

Response: 200 OK
{
  "idMembresia": 1,
  "idEstudiante": 1,
  ...
}
```

### Ejemplo 3: Registrar un Pago
```bash
POST /api/pagos
Content-Type: application/json

{
  "idEstudiante": 1,
  "mesPagado": "Enero 2025",
  "valor": 150000.00,
  "metodoPago": "ONLINE",
  "estadoPago": "PAGADO"
}

Response: 200 OK
{
  "idPago": 1,
  "idEstudiante": 1,
  "estadoPago": "PAGADO",
  ...
}
```

---

## 🧪 Pruebas Recomendadas

Usa **Postman**, **Insomnia** o **cURL** para probar:

```bash
# Obtener todos los roles
curl -X GET http://localhost:8080/api/roles

# Crear un nuevo rol
curl -X POST http://localhost:8080/api/roles \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Profesor"}'

# Obtener rol por ID
curl -X GET http://localhost:8080/api/roles/1

# Actualizar rol
curl -X PUT http://localhost:8080/api/roles/1 \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Entrenador"}'

# Eliminar rol
curl -X DELETE http://localhost:8080/api/roles/1
```

---

## ✨ Características de los Controladores

✅ **CRUD Completo**: Create, Read, Update, Delete
✅ **CORS habilitado**: Para acceso desde frontend
✅ **Búsquedas específicas**: Filtros por estado, sede, categoría, etc.
✅ **Métodos especiales**: Desactivar, cambiar estado, etc.
✅ **Manejo de errores**: Retorna 404 si no existe
✅ **Respuestas consistentes**: Siempre JSON

---

## 📱 Ejemplo de Respuesta

### GET /api/estudiantes/1

```json
{
  "idEstudiante": 1,
  "nombreCompleto": "Maria López",
  "tipoDocumento": "CC",
  "numeroDocumento": "1098765432",
  "fechaNacimiento": "2006-05-15",
  "edad": 18,
  "sexo": "FEMENINO",
  "direccionResidencia": "Calle 5 No 10",
  "barrio": "Centro",
  "celularEstudiante": "3101234567",
  "whatsappEstudiante": "3101234567",
  "correoEstudiante": "maria@example.com",
  "sede": {
    "idSede": 1,
    "nombre": "Sede Centro",
    "direccion": "Calle Principal 123",
    "telefono": "3001234567",
    "estado": true
  },
  "nombreTutor": "Pedro López",
  "parentescoTutor": "Padre",
  "documentoTutor": "9876543210",
  "telefonoTutor": "3001234568",
  "correoTutor": "pedro@example.com",
  "ocupacionTutor": "Ingeniero",
  "institucionEducativa": "Colegio Nacional",
  "jornada": "MAÑANA",
  "gradoActual": 11,
  "eps": "EPS SALUD",
  "tipoSangre": "O+",
  "alergias": "Penicilina",
  "nivel": "INTERMEDIO",
  "posicionPreferida": "Libero",
  "dominancia": "DERECHA",
  "estado": true
}
```

---

**API Versión:** 1.0
**Fecha:** 29 de Diciembre de 2025
**Estado:** ✅ Completada
