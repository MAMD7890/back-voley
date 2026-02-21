# 📋 PANEL ADMINISTRATIVO - GESTIÓN DE PLANES Y CONFIGURACIÓN

**Estado**: ✅ IMPLEMENTADO  
**Fecha**: 19 de Febrero de 2026  
**Framework**: Spring Boot 3.5.9

---

## 🎯 RESUMEN EJECUTIVO

Se han implementado dos nuevas entidades y sus respectivos endpoints REST para gestionar:

1. **Planes de Precios** - Crear, editar, eliminar planes de membresía (1, 2, 3 meses, etc.)
2. **Configuración del Sistema** - Gestionar precio de matrícula y otras configuraciones

Todo está protegido con autenticación JWT y autorización basada en roles (Solo ADMIN).

---

## 📊 ARQUITECTURA

### Entidades Creadas

#### 1. **Plan.java**
```
Tabla: plan
Campos:
- idPlan (PK)
- nombre (String, requerido, único)
- descripcion (String)
- duracionMeses (Integer, requerido)
- precio (BigDecimal, requerido)
- precioMensual (BigDecimal, calculado)
- descripcionCorta (String)
- activo (Boolean, default: true)
- masPopular (Boolean, default: false)
- ordenVisualizacion (Integer, para ordenar en UI)
- fechaCreacion (LocalDateTime)
- fechaActualizacion (LocalDateTime)
```

#### 2. **Configuracion.java**
```
Tabla: configuracion
Campos:
- idConfiguracion (PK)
- clave (String, requerido, único)
- descripcion (String)
- valor (String)
- tipo (String: STRING, BIGDECIMAL, INTEGER, BOOLEAN, JSON)
- fechaCreacion (LocalDateTime)
- fechaActualizacion (LocalDateTime)
```

### Componentes

| Componente | Clase | Ubicación |
|-----------|-------|-----------|
| Entidades | `Plan`, `Configuracion` | `entity/` |
| Repositorios | `PlanRepository`, `ConfiguracionRepository` | `repository/` |
| Servicios | `PlanService`, `ConfiguracionService` | `service/` |
| DTOs | `PlanDTO`, `ConfiguracionDTO` | `dto/` |
| Controllers | `PlanController`, `ConfiguracionController` | `controller/` |

---

## 🔌 ENDPOINTS REST

### 📌 PLANES (PlanController)

#### 1. Obtener todos los planes activos
```
GET /api/planes
Acceso: Público
Status: 200 OK

Response:
[
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
  },
  ...
]
```

#### 2. Obtener todos los planes (incluyendo inactivos) - ADMIN ONLY
```
GET /api/planes/admin/todos
Acceso: Solo ADMIN
Status: 200 OK

Response: [Array de PlanDTO]
```

#### 3. Obtener un plan específico
```
GET /api/planes/{id}
Acceso: Público
Status: 200 OK | 404 Not Found

Response:
{
  "idPlan": 1,
  "nombre": "Plan 1 mes",
  ...
}
```

#### 4. Crear nuevo plan - ADMIN ONLY
```
POST /api/planes
Acceso: Solo ADMIN
Content-Type: application/json
Status: 201 Created | 400 Bad Request

Request Body:
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

Response:
{
  "idPlan": 1,
  "nombre": "Plan 1 mes",
  "precio": 80000,
  "precioMensual": 80000,  // Calculado automáticamente
  ...
}

Validaciones:
✓ nombre: requerido, no vacío
✓ duracionMeses: > 0
✓ precio: > 0
✓ nombre debe ser único
```

#### 5. Actualizar un plan - ADMIN ONLY
```
PUT /api/planes/{id}
Acceso: Solo ADMIN
Content-Type: application/json
Status: 200 OK | 400 Bad Request

Request Body: (campos opcionales)
{
  "nombre": "Plan 1 mes - ACTUALIZADO",
  "precio": 85000,
  "masPopular": true
}

Response: PlanDTO actualizado
```

#### 6. Desactivar un plan (Soft delete) - ADMIN ONLY
```
PATCH /api/planes/{id}/desactivar
Acceso: Solo ADMIN
Status: 200 OK

Response:
{
  "idPlan": 1,
  "activo": false,
  ...
}
```

#### 7. Eliminar un plan (Hard delete) - ADMIN ONLY
```
DELETE /api/planes/{id}
Acceso: Solo ADMIN
Status: 200 OK

Response:
{
  "mensaje": "Plan eliminado correctamente"
}
```

---

### ⚙️ CONFIGURACIÓN (ConfiguracionController)

#### 1. Obtener todas las configuraciones - ADMIN ONLY
```
GET /api/configuracion
Acceso: Solo ADMIN
Status: 200 OK

Response:
[
  {
    "idConfiguracion": 1,
    "clave": "PRECIO_MATRICULA",
    "descripcion": "Precio de matrícula para nuevos estudiantes",
    "valor": "170000",
    "tipo": "BIGDECIMAL"
  },
  ...
]
```

#### 2. Obtener configuración por clave
```
GET /api/configuracion/{clave}
Acceso: Público
Status: 200 OK | 404 Not Found

Ejemplo:
GET /api/configuracion/PRECIO_MATRICULA

Response:
{
  "idConfiguracion": 1,
  "clave": "PRECIO_MATRICULA",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}
```

#### 3. Obtener precio de matrícula (Endpoint específico)
```
GET /api/configuracion/precio/matricula
Acceso: Público
Status: 200 OK

Response:
{
  "clave": "PRECIO_MATRICULA",
  "valor": 170000,
  "tipo": "BIGDECIMAL"
}
```

#### 4. Actualizar precio de matrícula - ADMIN ONLY
```
PATCH /api/configuracion/precio/matricula
Acceso: Solo ADMIN
Content-Type: application/json
Status: 200 OK

Request Body:
{
  "precio": "180000"
}

Response:
{
  "idConfiguracion": 1,
  "clave": "PRECIO_MATRICULA",
  "valor": "180000",
  "tipo": "BIGDECIMAL"
}
```

#### 5. Guardar o actualizar cualquier configuración - ADMIN ONLY
```
POST /api/configuracion
Acceso: Solo ADMIN
Content-Type: application/json
Status: 201 Created

Request Body:
{
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Precio de matrícula para nuevos estudiantes",
  "valor": "180000",
  "tipo": "BIGDECIMAL"
}

Response: ConfiguracionDTO guardada
```

#### 6. Actualizar configuración por clave - ADMIN ONLY
```
PUT /api/configuracion/{clave}
Acceso: Solo ADMIN
Content-Type: application/json
Status: 200 OK

Request Body:
{
  "valor": "180000",
  "tipo": "BIGDECIMAL"
}

Response: ConfiguracionDTO actualizada
```

#### 7. Eliminar configuración - ADMIN ONLY
```
DELETE /api/configuracion/{id}
Acceso: Solo ADMIN
Status: 200 OK

Response:
{
  "mensaje": "Configuración eliminada correctamente"
}
```

---

## 🛡️ SEGURIDAD

### Autenticación
- Todos los endpoints requieren **JWT Bearer Token** en header
- Formato: `Authorization: Bearer {token}`

### Autorización
- **Público**: Obtener planes activos, obtener precio de matrícula
- **ADMIN ONLY**: Todas las operaciones de escritura (crear, editar, eliminar)
- Implementado con `@PreAuthorize("hasRole('ADMIN')")`

### Validaciones
- Campos requeridos validados
- Valores numéricos validados
- Unicidad de claves en Configuración
- Unicidad de nombres en Planes

---

## 📝 EJEMPLO DE USO - ADMIN PANEL

### Caso 1: Crear los 3 planes iniciales

```bash
# Plan 1 mes
POST /api/planes
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

# Plan 2 meses (más popular)
POST /api/planes
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

# Plan 3 meses
POST /api/planes
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

### Caso 2: Actualizar precio de matrícula

```bash
PATCH /api/configuracion/precio/matricula
{
  "precio": "175000"
}
```

### Caso 3: Editar un plan (cambiar precio)

```bash
PUT /api/planes/1
{
  "precio": 85000
}
```

### Caso 4: Marcar plan como más popular

```bash
PUT /api/planes/1
{
  "masPopular": true
}
```

---

## 🗄️ DATOS INICIALES RECOMENDADOS

Para iniciar, ejecutar estas requests en el orden indicado:

### 1. Crear Configuración de Matrícula
```json
POST /api/configuracion
{
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Precio de matrícula para nuevos estudiantes",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}
```

### 2. Crear los 3 Planes
Ver sección "Caso 1" arriba

---

## 📱 INTEGRACIÓN CON FRONTEND

### Obtener planes para mostrar tarjetas
```typescript
// Component de precios
async obtenerPlanes() {
  const response = await fetch('/api/planes');
  return await response.json();
}
```

### Obtener precio de matrícula
```typescript
// Al crear nuevo estudiante
async obtenerPrecioMatricula() {
  const response = await fetch('/api/configuracion/precio/matricula');
  const data = await response.json();
  return data.valor; // 170000
}
```

### Panel de administrador - Editar plan
```typescript
// Admin: actualizar precio del plan
async actualizarPlan(idPlan, datos) {
  const token = localStorage.getItem('token');
  const response = await fetch(`/api/planes/${idPlan}`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(datos)
  });
  return await response.json();
}
```

### Panel de administrador - Editar matrícula
```typescript
// Admin: actualizar precio de matrícula
async actualizarMatricula(nuevoPrecio) {
  const token = localStorage.getItem('token');
  const response = await fetch('/api/configuracion/precio/matricula', {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ precio: nuevoPrecio.toString() })
  });
  return await response.json();
}
```

---

## 🐛 MANEJO DE ERRORES

### Errores comunes

| Error | Status | Causa | Solución |
|-------|--------|-------|----------|
| `Plan no encontrado` | 404 | El ID no existe | Verificar ID del plan |
| `Ya existe un plan con ese nombre` | 400 | Nombre duplicado | Usar nombre único |
| `La duración en meses debe ser mayor a 0` | 400 | Valor inválido | Enviar duracionMeses > 0 |
| `El precio debe ser mayor a 0` | 400 | Valor inválido | Enviar precio > 0 |
| `Acceso denegado` | 403 | No es ADMIN | Usar cuenta de administrador |
| `Token no válido` | 401 | JWT expirado | Autenticarse nuevamente |

---

## 🔄 ACTUALIZACIÓN DE PRECIOS EN TIEMPO REAL

Cuando se actualiza un precio:

1. **El plan** recalcula `precioMensual` automáticamente
2. **El precio de matrícula** se aplica al crear nuevos estudiantes
3. **Los planes activos** son mostrados por orden de visualización
4. **El badge "Más Popular"** puede cambiar según necesidad

---

## 📌 NOTAS IMPORTANTES

1. **Transacciones**: Todas las operaciones de escritura usan `@Transactional`
2. **Auditoría**: Se registra `fechaCreacion` y `fechaActualizacion` automáticamente
3. **Soft Delete**: Usar `PATCH /planes/{id}/desactivar` en lugar de DELETE para seguridad
4. **Cálculo Automático**: El `precioMensual` se calcula al crear o actualizar un plan
5. **Orden**: `ordenVisualizacion` controla el orden en que aparecen las tarjetas en el frontend

---

## ✅ ESTADO DE IMPLEMENTACIÓN

| Componente | Estado | Tests |
|-----------|--------|-------|
| Entidades | ✅ COMPLETO | N/A |
| Repositories | ✅ COMPLETO | N/A |
| Servicios | ✅ COMPLETO | N/A |
| DTOs | ✅ COMPLETO | N/A |
| Controllers | ✅ COMPLETO | N/A |
| Compilación | ✅ BUILD SUCCESS | ✅ |
| Endpoints | ✅ 14 endpoints | ✅ |

---

**Documento**: Panel Admin - Gestión de Planes  
**Versión**: 1.0  
**Última actualización**: 19 de Febrero de 2026
