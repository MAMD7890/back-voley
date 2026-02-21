# 🎯 GUÍA DE INTEGRACIÓN FRONTEND - PLANES Y CONFIGURACIÓN

**Fecha**: 19 de Febrero de 2026  
**Backend**: Spring Boot 3.5.9  
**Frontend**: Angular 17  
**Base de Datos**: MySQL 8.0+

---

## 📋 RESUMEN EJECUTIVO

Se han implementado **2 nuevas funcionalidades** en el backend para que puedas gestionar desde el panel administrativo:

### 1. **Gestión de Planes de Membresía**
- Crear, editar, eliminar planes (1 mes, 2 meses, 3 meses, etc.)
- Cada plan tiene su precio total, precio mensual calculado automáticamente
- Marcar planes como "Más Popular"
- Activar/Desactivar planes
- Ordenar planes en la UI

### 2. **Gestión de Configuración del Sistema**
- Gestionar precio de matrícula
- Otras configuraciones del sistema (extensibles)

---

## 🏗️ CLASES CREADAS EN EL BACKEND

### **Entidades (Entity)**

#### 1. `Plan.java`
```java
package galacticos_app_back.galacticos.entity;

@Entity
@Table(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlan;
    
    private String nombre;                    // "Plan 1 mes"
    private String descripcion;              // "Acceso completo a entrenamientos"
    private Integer duracionMeses;           // 1, 2, 3, etc.
    private BigDecimal precio;               // 80000, 150000, 210000
    private BigDecimal precioMensual;        // Auto-calculado (precio / meses)
    private String descripcionCorta;         // "1 mes de clases"
    private Boolean activo;                  // true/false
    private Boolean masPopular;              // true para mostrar badge
    private Integer ordenVisualizacion;      // 0, 1, 2, ... (para ordenar en UI)
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
```

#### 2. `Configuracion.java`
```java
package galacticos_app_back.galacticos.entity;

@Entity
@Table(name = "configuracion")
public class Configuracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConfiguracion;
    
    private String clave;          // "PRECIO_MATRICULA"
    private String descripcion;    // "Precio de matrícula para nuevos estudiantes"
    private String valor;          // "170000"
    private String tipo;           // "BIGDECIMAL", "STRING", etc.
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
```

---

## 📊 DTOs (Modelos de Transferencia)

### `PlanDTO.java`
```typescript
// Lo que recibirás del backend
interface PlanDTO {
  idPlan: number;
  nombre: string;                    // "Plan 1 mes"
  descripcion: string;              // "Acceso completo a entrenamientos"
  duracionMeses: number;            // 1
  precio: number;                   // 80000
  precioMensual: number;            // 80000
  descripcionCorta: string;         // "1 mes de clases"
  activo: boolean;                  // true
  masPopular: boolean;              // false
  ordenVisualizacion: number;       // 0
}
```

### `ConfiguracionDTO.java`
```typescript
// Lo que recibirás del backend
interface ConfiguracionDTO {
  idConfiguracion: number;
  clave: string;                    // "PRECIO_MATRICULA"
  descripcion: string;
  valor: string;                    // "170000"
  tipo: string;                     // "BIGDECIMAL"
}
```

---

## 🔌 ENDPOINTS REST

### 📌 **PLANES** (PlanController)

#### 1️⃣ Obtener todos los planes activos
```
GET /api/planes
Autenticación: NO requerida
Respuesta: Array<PlanDTO>

Ejemplo en Frontend:
```typescript
getPlanesActivos() {
  return this.http.get<PlanDTO[]>('/api/planes');
}
```

#### 2️⃣ Obtener todos los planes (incluyendo inactivos) - ADMIN ONLY
```
GET /api/planes/admin/todos
Autenticación: Bearer Token (ADMIN)
Respuesta: Array<PlanDTO>

Ejemplo en Frontend:
```typescript
getTodosLosPlanes() {
  const headers = this.getAuthHeaders();
  return this.http.get<PlanDTO[]>('/api/planes/admin/todos', { headers });
}
```

#### 3️⃣ Obtener un plan específico
```
GET /api/planes/{id}
Autenticación: NO requerida
Respuesta: PlanDTO

Ejemplo:
GET /api/planes/1
```

#### 4️⃣ Crear nuevo plan - ADMIN ONLY
```
POST /api/planes
Autenticación: Bearer Token (ADMIN)
Content-Type: application/json

Body:
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

Response: PlanDTO (con idPlan asignado)

Ejemplo en Frontend:
```typescript
crearPlan(plan: PlanDTO) {
  const headers = this.getAuthHeaders();
  return this.http.post<PlanDTO>('/api/planes', plan, { headers });
}
```

#### 5️⃣ Actualizar un plan - ADMIN ONLY
```
PUT /api/planes/{id}
Autenticación: Bearer Token (ADMIN)

Body (solo los campos a actualizar):
{
  "precio": 85000,
  "masPopular": true
}

Response: PlanDTO actualizado

Ejemplo:
```typescript
actualizarPlan(id: number, planActualizado: Partial<PlanDTO>) {
  const headers = this.getAuthHeaders();
  return this.http.put<PlanDTO>(`/api/planes/${id}`, planActualizado, { headers });
}
```

#### 6️⃣ Desactivar un plan (Soft delete) - ADMIN ONLY
```
PATCH /api/planes/{id}/desactivar
Autenticación: Bearer Token (ADMIN)
Body: (vacío)

Response:
{
  "idPlan": 1,
  "activo": false,
  ...
}

Ejemplo:
```typescript
desactivarPlan(id: number) {
  const headers = this.getAuthHeaders();
  return this.http.patch(`/api/planes/${id}/desactivar`, {}, { headers });
}
```

#### 7️⃣ Eliminar un plan (Hard delete) - ADMIN ONLY
```
DELETE /api/planes/{id}
Autenticación: Bearer Token (ADMIN)

Response:
{ "mensaje": "Plan eliminado correctamente" }

Ejemplo:
```typescript
eliminarPlan(id: number) {
  const headers = this.getAuthHeaders();
  return this.http.delete(`/api/planes/${id}`, { headers });
}
```

---

### ⚙️ **CONFIGURACIÓN** (ConfiguracionController)

#### 1️⃣ Obtener todas las configuraciones - ADMIN ONLY
```
GET /api/configuracion
Autenticación: Bearer Token (ADMIN)
Respuesta: Array<ConfiguracionDTO>
```

#### 2️⃣ Obtener configuración por clave
```
GET /api/configuracion/{clave}
Autenticación: NO requerida

Ejemplo:
GET /api/configuracion/PRECIO_MATRICULA

Response:
{
  "idConfiguracion": 1,
  "clave": "PRECIO_MATRICULA",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}

Ejemplo en Frontend:
```typescript
obtenerConfiguracion(clave: string) {
  return this.http.get<ConfiguracionDTO>(`/api/configuracion/${clave}`);
}
```

#### 3️⃣ Obtener precio de matrícula (Endpoint específico)
```
GET /api/configuracion/precio/matricula
Autenticación: NO requerida

Response:
{
  "clave": "PRECIO_MATRICULA",
  "valor": 170000,
  "tipo": "BIGDECIMAL"
}

Ejemplo en Frontend:
```typescript
obtenerPrecioMatricula() {
  return this.http.get<{ valor: number }>('/api/configuracion/precio/matricula');
}
```

#### 4️⃣ Actualizar precio de matrícula - ADMIN ONLY
```
PATCH /api/configuracion/precio/matricula
Autenticación: Bearer Token (ADMIN)

Body:
{
  "precio": "180000"
}

Response: ConfiguracionDTO actualizado

Ejemplo en Frontend:
```typescript
actualizarPrecioMatricula(nuevoPrecio: number) {
  const headers = this.getAuthHeaders();
  return this.http.patch(
    '/api/configuracion/precio/matricula',
    { precio: nuevoPrecio.toString() },
    { headers }
  );
}
```

#### 5️⃣ Guardar/Actualizar configuración genérica - ADMIN ONLY
```
POST /api/configuracion
Autenticación: Bearer Token (ADMIN)

Body:
{
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Precio de matrícula",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}

Response: ConfiguracionDTO guardada
```

---

## 🎨 COMPONENTES ANGULAR RECOMENDADOS

### **1. Servicio para Planes**

```typescript
// src/app/services/plan.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

interface PlanDTO {
  idPlan?: number;
  nombre: string;
  descripcion: string;
  duracionMeses: number;
  precio: number;
  precioMensual?: number;
  descripcionCorta: string;
  activo: boolean;
  masPopular: boolean;
  ordenVisualizacion: number;
}

@Injectable({
  providedIn: 'root'
})
export class PlanService {
  private apiUrl = 'http://localhost:8080/api/planes';

  constructor(private http: HttpClient) { }

  // Obtener planes activos (público)
  getPlanesActivos(): Observable<PlanDTO[]> {
    return this.http.get<PlanDTO[]>(this.apiUrl);
  }

  // Obtener todos los planes (admin)
  getTodosLosPlanes(): Observable<PlanDTO[]> {
    return this.http.get<PlanDTO[]>(`${this.apiUrl}/admin/todos`, {
      headers: this.getAuthHeaders()
    });
  }

  // Obtener un plan específico
  getPlan(id: number): Observable<PlanDTO> {
    return this.http.get<PlanDTO>(`${this.apiUrl}/${id}`);
  }

  // Crear plan (admin)
  crearPlan(plan: PlanDTO): Observable<PlanDTO> {
    return this.http.post<PlanDTO>(this.apiUrl, plan, {
      headers: this.getAuthHeaders()
    });
  }

  // Actualizar plan (admin)
  actualizarPlan(id: number, plan: Partial<PlanDTO>): Observable<PlanDTO> {
    return this.http.put<PlanDTO>(`${this.apiUrl}/${id}`, plan, {
      headers: this.getAuthHeaders()
    });
  }

  // Desactivar plan (admin)
  desactivarPlan(id: number): Observable<PlanDTO> {
    return this.http.patch<PlanDTO>(`${this.apiUrl}/${id}/desactivar`, {}, {
      headers: this.getAuthHeaders()
    });
  }

  // Eliminar plan (admin)
  eliminarPlan(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }
}
```

### **2. Servicio para Configuración**

```typescript
// src/app/services/configuracion.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

interface ConfiguracionDTO {
  idConfiguracion?: number;
  clave: string;
  descripcion?: string;
  valor: string;
  tipo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfiguracionService {
  private apiUrl = 'http://localhost:8080/api/configuracion';

  constructor(private http: HttpClient) { }

  // Obtener todas las configuraciones (admin)
  obtenerTodas(): Observable<ConfiguracionDTO[]> {
    return this.http.get<ConfiguracionDTO[]>(this.apiUrl, {
      headers: this.getAuthHeaders()
    });
  }

  // Obtener configuración por clave
  obtenerPorClave(clave: string): Observable<ConfiguracionDTO> {
    return this.http.get<ConfiguracionDTO>(`${this.apiUrl}/${clave}`);
  }

  // Obtener precio de matrícula
  obtenerPrecioMatricula(): Observable<{ clave: string; valor: number; tipo: string }> {
    return this.http.get<any>(`${this.apiUrl}/precio/matricula`);
  }

  // Actualizar precio de matrícula (admin)
  actualizarPrecioMatricula(precio: number): Observable<ConfiguracionDTO> {
    return this.http.patch<ConfiguracionDTO>(
      `${this.apiUrl}/precio/matricula`,
      { precio: precio.toString() },
      { headers: this.getAuthHeaders() }
    );
  }

  // Guardar configuración genérica (admin)
  guardarConfiguracion(config: ConfiguracionDTO): Observable<ConfiguracionDTO> {
    return this.http.post<ConfiguracionDTO>(this.apiUrl, config, {
      headers: this.getAuthHeaders()
    });
  }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }
}
```

---

## 💻 COMPONENTES DE UI RECOMENDADOS

### **3. Componente: Lista de Planes (Público)**

```typescript
// src/app/components/planes/planes-publicos.component.ts

import { Component, OnInit } from '@angular/core';
import { PlanService } from '../../services/plan.service';

@Component({
  selector: 'app-planes-publicos',
  templateUrl: './planes-publicos.component.html',
  styleUrls: ['./planes-publicos.component.css']
})
export class PlanesPublicosComponent implements OnInit {
  planes: any[] = [];
  cargando = false;
  error: string | null = null;

  constructor(private planService: PlanService) { }

  ngOnInit() {
    this.cargarPlanes();
  }

  cargarPlanes() {
    this.cargando = true;
    this.error = null;
    this.planService.getPlanesActivos().subscribe({
      next: (datos) => {
        this.planes = datos.sort((a, b) => a.ordenVisualizacion - b.ordenVisualizacion);
        this.cargando = false;
      },
      error: (err) => {
        this.error = 'Error al cargar planes';
        this.cargando = false;
        console.error(err);
      }
    });
  }
}
```

```html
<!-- src/app/components/planes/planes-publicos.component.html -->

<div class="planes-container">
  <div *ngIf="cargando" class="loading">Cargando planes...</div>
  
  <div *ngIf="error" class="error">{{ error }}</div>

  <div class="planes-grid">
    <div *ngFor="let plan of planes" class="plan-card" 
         [class.mas-popular]="plan.masPopular">
      
      <!-- Badge más popular -->
      <div *ngIf="plan.masPopular" class="badge">MÁS POPULAR</div>

      <!-- Logo -->
      <div class="plan-icon">⚽</div>

      <!-- Nombre -->
      <h3>{{ plan.nombre }}</h3>

      <!-- Descripción corta -->
      <p class="descripcion-corta">{{ plan.descripcionCorta }}</p>

      <!-- Precio -->
      <div class="precio">
        <span class="moneda">$</span>
        <span class="valor">{{ plan.precio | number }}</span>
        <span class="total">/total</span>
      </div>

      <!-- Incluir matrícula -->
      <label class="checkbox">
        <input type="checkbox" />
        <span>Incluir matrícula (+$170.000)</span>
      </label>

      <!-- Características -->
      <ul class="caracteristicas">
        <li>✓ {{ plan.duracionMeses }} meses de clases</li>
        <li>✓ Acceso completo a entrenamientos</li>
      </ul>

      <!-- Botón -->
      <button class="btn-comprar">Comprar ahora</button>
    </div>
  </div>
</div>
```

### **4. Componente: Admin - Gestionar Planes**

```typescript
// src/app/components/admin/admin-planes.component.ts

import { Component, OnInit } from '@angular/core';
import { PlanService } from '../../services/plan.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-admin-planes',
  templateUrl: './admin-planes.component.html'
})
export class AdminPlanesComponent implements OnInit {
  planes: any[] = [];
  formulario: FormGroup;
  editando = false;
  idEditando: number | null = null;

  constructor(
    private planService: PlanService,
    private fb: FormBuilder
  ) {
    this.formulario = this.fb.group({
      nombre: ['', Validators.required],
      duracionMeses: [1, Validators.required],
      precio: [0, Validators.required],
      descripcionCorta: [''],
      activo: [true],
      masPopular: [false],
      ordenVisualizacion: [0]
    });
  }

  ngOnInit() {
    this.cargarPlanes();
  }

  cargarPlanes() {
    this.planService.getTodosLosPlanes().subscribe({
      next: (datos) => this.planes = datos,
      error: (err) => console.error('Error:', err)
    });
  }

  guardarPlan() {
    if (!this.formulario.valid) return;

    const plan = this.formulario.value;

    if (this.editando && this.idEditando) {
      this.planService.actualizarPlan(this.idEditando, plan).subscribe({
        next: () => {
          this.cargarPlanes();
          this.limpiarFormulario();
          alert('Plan actualizado');
        },
        error: (err) => alert('Error: ' + err.error.error)
      });
    } else {
      this.planService.crearPlan(plan).subscribe({
        next: () => {
          this.cargarPlanes();
          this.limpiarFormulario();
          alert('Plan creado');
        },
        error: (err) => alert('Error: ' + err.error.error)
      });
    }
  }

  editarPlan(plan: any) {
    this.editando = true;
    this.idEditando = plan.idPlan;
    this.formulario.patchValue(plan);
  }

  eliminarPlan(id: number) {
    if (confirm('¿Estás seguro?')) {
      this.planService.eliminarPlan(id).subscribe({
        next: () => {
          this.cargarPlanes();
          alert('Plan eliminado');
        },
        error: (err) => alert('Error: ' + err.error.error)
      });
    }
  }

  limpiarFormulario() {
    this.editando = false;
    this.idEditando = null;
    this.formulario.reset({ activo: true, masPopular: false, ordenVisualizacion: 0 });
  }
}
```

---

## 🎨 CÓDIGO HTML RECOMENDADO

### **Tarjeta de Plan** (Para la imagen adjunta)

```html
<div class="plan-card" [class.mas-popular]="plan.masPopular">
  <!-- Badge -->
  <div *ngIf="plan.masPopular" class="badge-popular">
    MÁS POPULAR
  </div>

  <!-- Logo -->
  <div class="plan-logo">
    <img src="assets/logo.svg" alt="Galácticos">
  </div>

  <!-- Nombre -->
  <h3 class="plan-title">{{ plan.nombre }}</h3>

  <!-- Descripción -->
  <p class="plan-description">{{ plan.descripcionCorta }}</p>

  <!-- Precio -->
  <div class="plan-price">
    <span class="currency">$</span>
    <span class="amount">{{ plan.precio | number }}</span>
    <span class="period">/total</span>
  </div>

  <!-- Opción matrícula -->
  <div class="matricula-option">
    <input type="checkbox" id="matricula-{{ plan.idPlan }}" />
    <label [for]="'matricula-' + plan.idPlan">
      Incluir matrícula (+$170.000)
    </label>
  </div>

  <!-- Características -->
  <ul class="features">
    <li>
      <span class="checkmark">✓</span>
      <span>{{ plan.duracionMeses }} meses de clases</span>
    </li>
    <li>
      <span class="checkmark">✓</span>
      <span>Acceso completo a entrenamientos</span>
    </li>
  </ul>

  <!-- Botón -->
  <button class="btn-primary btn-full">Comprar ahora</button>
</div>
```

### **CSS recomendado**

```css
.plan-card {
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  background: white;
}

.plan-card.mas-popular {
  border-color: #4a90e2;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.15);
  transform: scale(1.05);
}

.badge-popular {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: #4a90e2;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.plan-price {
  font-size: 32px;
  font-weight: bold;
  margin: 16px 0;
  color: #333;
}

.plan-price .currency {
  font-size: 20px;
}

.plan-price .period {
  font-size: 14px;
  color: #999;
}

.features {
  list-style: none;
  padding: 0;
  margin: 16px 0;
  text-align: left;
}

.features li {
  padding: 8px 0;
  display: flex;
  align-items: center;
  color: #666;
}

.checkmark {
  color: #4caf50;
  font-weight: bold;
  margin-right: 8px;
}
```

---

## 📌 DATOS INICIALES - CÓMO CARGAR

```bash
# 1. Crear configuración de matrícula (Primero)
curl -X POST http://localhost:8080/api/configuracion \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "clave": "PRECIO_MATRICULA",
    "descripcion": "Precio de matrícula para nuevos estudiantes",
    "valor": "170000",
    "tipo": "BIGDECIMAL"
  }'

# 2. Crear Plan 1 mes
curl -X POST http://localhost:8080/api/planes \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Plan 1 mes",
    "descripcion": "Acceso completo a entrenamientos",
    "duracionMeses": 1,
    "precio": 80000,
    "descripcionCorta": "1 mes de clases",
    "activo": true,
    "masPopular": false,
    "ordenVisualizacion": 0
  }'

# 3. Crear Plan 2 meses (POPULAR)
curl -X POST http://localhost:8080/api/planes \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Plan 2 meses",
    "descripcion": "Acceso completo a entrenamientos",
    "duracionMeses": 2,
    "precio": 150000,
    "descripcionCorta": "2 meses de clases",
    "activo": true,
    "masPopular": true,
    "ordenVisualizacion": 1
  }'

# 4. Crear Plan 3 meses
curl -X POST http://localhost:8080/api/planes \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Plan 3 meses",
    "descripcion": "Acceso completo a entrenamientos",
    "duracionMeses": 3,
    "precio": 210000,
    "descripcionCorta": "3 meses de clases",
    "activo": true,
    "masPopular": false,
    "ordenVisualizacion": 2
  }'
```

---

## 🔐 AUTENTICACIÓN

### Cómo obtener el token
```typescript
// En tu auth.service.ts
login(usuario: string, contraseña: string) {
  return this.http.post('/api/auth/login', {
    usuario,
    contraseña
  });
}

// Guardar el token
guardarToken(token: string) {
  localStorage.setItem('token', token);
}

// Obtener el token
obtenerToken() {
  return localStorage.getItem('token');
}
```

### Usar el token en requests
```typescript
const token = localStorage.getItem('token');
const headers = new HttpHeaders({
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
});
```

---

## 📊 FLUJOS DE USUARIO

### **Flujo 1: Ver Planes (Público)**
1. Usuario abre la página de precios
2. Frontend llama: `GET /api/planes`
3. Se muestran los 3 planes ordenados por `ordenVisualizacion`
4. El plan con `masPopular=true` muestra badge "MÁS POPULAR"

### **Flujo 2: Admin Edita Precio de Plan**
1. Admin entra al panel administrativo
2. Ve lista de planes: `GET /api/planes/admin/todos`
3. Hace clic en "Editar" de un plan
4. Modifica el precio
5. Frontend envía: `PUT /api/planes/{id}` con nuevo precio
6. Se recalcula `precioMensual` automáticamente

### **Flujo 3: Admin Actualiza Matrícula**
1. Admin entra a configuración
2. Ve el precio actual: `GET /api/configuracion/precio/matricula`
3. Cambia el valor
4. Frontend envía: `PATCH /api/configuracion/precio/matricula`
5. Se actualiza en BD

---

## ❌ MANEJO DE ERRORES

### Errores comunes que puede recibir:

```typescript
// Error 400 - Validación
{
  "error": "El nombre del plan es requerido"
}

// Error 400 - Duplicado
{
  "error": "Ya existe un plan con ese nombre"
}

// Error 403 - Sin permisos
{
  "error": "Acceso denegado"
}

// Error 404 - No encontrado
{
  "error": "Plan no encontrado"
}

// Error 401 - Token expirado
{
  "error": "Token no válido"
}
```

### Cómo manejar errores en Angular:
```typescript
this.planService.crearPlan(plan).subscribe({
  next: (resultado) => {
    console.log('Plan creado:', resultado);
  },
  error: (error) => {
    const mensaje = error.error?.error || 'Error desconocido';
    console.error('Error:', mensaje);
    this.mostrarAlerta(mensaje);
  }
});
```

---

## 📝 CHECKLIST PARA FRONTEND

- [ ] Crear servicio `PlanService`
- [ ] Crear servicio `ConfiguracionService`
- [ ] Crear componente para mostrar planes (público)
- [ ] Crear componente admin para gestionar planes
- [ ] Crear componente admin para gestionar matrícula
- [ ] Implementar formularios de creación/edición
- [ ] Implementar modal de confirmación para eliminar
- [ ] Agregar validaciones en formularios
- [ ] Manejar errores con mensajes al usuario
- [ ] Agregar loading spinners
- [ ] Probar con datos iniciales
- [ ] Integrar con sistema de autenticación

---

## 🚀 PRÓXIMAS FASES (Opcionales)

1. **Pagos Wompi** - Integración con planes
2. **Reportes** - Reportes de ventas por plan
3. **Cupones** - Sistema de descuentos
4. **Historial** - Historial de cambios de precios
5. **Analytics** - Dashboard de métricas

---

**Documento preparado para**: Equipo Frontend Angular 17  
**Fecha**: 19 de Febrero de 2026  
**Versión**: 1.0
