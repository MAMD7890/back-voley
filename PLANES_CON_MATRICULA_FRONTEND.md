# Integración de Planes con Valor de Matrícula - Frontend

## 📋 Descripción

El endpoint de planes ahora incluye automáticamente el **valor de matrícula** y su **descripción** en cada respuesta. Esto permite mostrar el costo total (plan + matrícula) en el formulario del frontend.

## 🔗 Endpoints Actualizados

### GET /api/planes (Público)
Obtiene todos los planes activos con información de matrícula.

**Respuesta:**
```json
[
  {
    "idPlan": 1,
    "nombre": "Plan 1 Mes",
    "descripcion": "Acceso completo por 1 mes",
    "duracionMeses": 1,
    "precio": 49900,
    "precioMensual": 49900,
    "descripcionCorta": "1 mes de acceso",
    "activo": true,
    "masPopular": false,
    "ordenVisualizacion": 1,
    "precioMatricula": 170000,
    "descripcionMatricula": "Acceso a plataforma educativa Galácticos"
  },
  {
    "idPlan": 2,
    "nombre": "Plan 3 Meses",
    "descripcion": "Acceso completo por 3 meses",
    "duracionMeses": 3,
    "precio": 119900,
    "precioMensual": 39966.67,
    "descripcionCorta": "3 meses de acceso",
    "activo": true,
    "masPopular": true,
    "ordenVisualizacion": 2,
    "precioMatricula": 170000,
    "descripcionMatricula": "Acceso a plataforma educativa Galácticos"
  }
]
```

### GET /api/planes/{id} (Público)
Obtiene un plan específico con información de matrícula.

### GET /api/planes/admin/todos (USER/ADMIN)
Obtiene todos los planes (incluidos inactivos) con información de matrícula.

### POST /api/planes (USER/ADMIN)
Crea un nuevo plan. El DTO incluye campos de matrícula (ignorados en la creación):

**Body:**
```json
{
  "nombre": "Plan 2 Meses",
  "descripcion": "Acceso por 2 meses",
  "duracionMeses": 2,
  "precio": 89900,
  "descripcionCorta": "2 meses",
  "activo": true,
  "masPopular": false,
  "ordenVisualizacion": 1
}
```

**Respuesta:** Incluye automáticamente `precioMatricula` y `descripcionMatricula`.

### PUT /api/planes/{id} (USER/ADMIN)
Actualiza un plan existente. Recibe el DTO completo pero ignora los campos de matrícula.

### PATCH /api/planes/{id}/desactivar (USER/ADMIN)
Desactiva un plan (soft delete). Respuesta incluye información de matrícula.

### DELETE /api/planes/{id} (USER/ADMIN)
Elimina un plan completamente.

## 💰 Cálculo Total en Frontend

```typescript
// Typescript/Angular
export interface Plan {
  idPlan: number;
  nombre: string;
  duracionMeses: number;
  precio: number;           // Precio del plan
  precioMensual: number;
  precioMatricula: number;  // Nuevo: Valor de matrícula
  descripcionMatricula: string;
  activo: boolean;
  masPopular: boolean;
  ordenVisualizacion: number;
}

// Calcular total con matrícula
calcularTotal(plan: Plan): number {
  return plan.precio + plan.precioMatricula;
}

// Mostrar en template
<div class="plan-total">
  <p>Plan: ${{ plan.precio | currency }}</p>
  <p>Matrícula: ${{ plan.precioMatricula | currency }}</p>
  <hr />
  <strong>Total: ${{ calcularTotal(plan) | currency }}</strong>
</div>
```

## 🎯 Detalles Técnicos

### Origen de los Datos de Matrícula

Los valores `precioMatricula` y `descripcionMatricula` se cargan desde la tabla `configuracion`:

- **precioMatricula**: Se obtiene de la configuración con clave `PRECIO_MATRICULA`
- **descripcionMatricula**: Se obtiene de la configuración con clave `DESCRIPCION_MATRICULA`

### Inicialización de Datos

Si no existen estas configuraciones, el sistema devuelve:
- `precioMatricula`: `null` o `0`
- `descripcionMatricula`: `"Matrícula"` (default)

Para inicializar estos valores, usa el endpoint de configuración:

```bash
POST /api/configuracion
Authorization: Bearer TOKEN_USER_O_ADMIN
Content-Type: application/json

{
  "clave": "PRECIO_MATRICULA",
  "descripcion": "Valor de matrícula para nuevos estudiantes",
  "valor": "170000",
  "tipo": "BIGDECIMAL"
}

{
  "clave": "DESCRIPCION_MATRICULA",
  "descripcion": "Descripción del valor de matrícula",
  "valor": "Acceso a plataforma educativa Galácticos",
  "tipo": "STRING"
}
```

## 📱 Ejemplo en Angular Component

```typescript
import { Component, OnInit } from '@angular/core';
import { PlanService } from './services/plan.service';

interface Plan {
  idPlan: number;
  nombre: string;
  precio: number;
  duracionMeses: number;
  precioMatricula: number;
  descripcionMatricula: string;
  precioMensual: number;
  masPopular: boolean;
}

@Component({
  selector: 'app-planes',
  template: `
    <div class="planes-container">
      <div *ngFor="let plan of planes" class="plan-card">
        <h3>{{ plan.nombre }}</h3>
        <div class="prices">
          <div class="price-item">
            <label>Plan:</label>
            <span>${{ plan.precio | currency }}</span>
          </div>
          <div class="price-item">
            <label>{{ plan.descripcionMatricula }}:</label>
            <span>${{ plan.precioMatricula | currency }}</span>
          </div>
          <div class="total">
            <strong>Total:</strong>
            <strong>${{ (plan.precio + plan.precioMatricula) | currency }}</strong>
          </div>
        </div>
        <button (click)="seleccionarPlan(plan)">Seleccionar</button>
      </div>
    </div>
  `,
  styles: [`
    .plan-card {
      border: 1px solid #ddd;
      padding: 20px;
      margin: 10px;
      border-radius: 8px;
    }
    .prices {
      margin: 15px 0;
    }
    .price-item {
      display: flex;
      justify-content: space-between;
      margin: 8px 0;
    }
    .total {
      border-top: 2px solid #007bff;
      padding-top: 10px;
      margin-top: 10px;
      display: flex;
      justify-content: space-between;
    }
  `]
})
export class PlanesComponent implements OnInit {
  planes: Plan[] = [];

  constructor(private planService: PlanService) {}

  ngOnInit() {
    this.cargarPlanes();
  }

  cargarPlanes() {
    this.planService.obtenerPlanes().subscribe(
      (data: Plan[]) => {
        this.planes = data;
      },
      (error) => console.error('Error al cargar planes:', error)
    );
  }

  seleccionarPlan(plan: Plan) {
    const total = plan.precio + plan.precioMatricula;
    console.log(`Plan seleccionado: ${plan.nombre}`);
    console.log(`Total a pagar: $${total}`);
    // Proceder con el pago o registro
  }
}
```

## 🔄 Migración de Código Existente

Si ya tienes código que consume `/api/planes`, **es compatible hacia atrás**:

```typescript
// Código antiguo (sigue funcionando)
interface OldPlan {
  idPlan: number;
  nombre: string;
  precio: number;
  // ... otros campos
}

// Nuevo (con matrícula)
interface NewPlan extends OldPlan {
  precioMatricula: number;
  descripcionMatricula: string;
}

// El código antiguo sigue funcionando, simplemente ignora los nuevos campos
```

## ✅ Checklist de Implementación

- [ ] Actualizar servicio de planes en Angular para obtener datos
- [ ] Crear componente para mostrar planes con matrícula
- [ ] Implementar cálculo de total (plan + matrícula)
- [ ] Mostrar desglose de precios en formulario de registro
- [ ] Actualizar carrito de compra si existe
- [ ] Inicializar configuración de matrícula en BD
- [ ] Probar con diferentes valores de matrícula
- [ ] Validar que la matrícula se cargue correctamente

## 🐛 Troubleshooting

### La matrícula viene como null
**Causa:** La configuración no existe en la BD.
**Solución:** Crea los registros en la tabla `configuracion` con las claves `PRECIO_MATRICULA` y `DESCRIPCION_MATRICULA`.

### El total no se calcula correctamente
**Causa:** Posible error de tipos en TypeScript.
**Solución:** Asegúrate de convertir a número antes de sumar:
```typescript
const total = Number(plan.precio) + Number(plan.precioMatricula);
```

### No veo la descripción de matrícula
**Causa:** No existe la configuración o está vacía.
**Solución:** Actualiza la configuración con una descripción válida.

---

**Última actualización:** 2026-02-19  
**Backend:** Spring Boot 3.5.9  
**Frontend:** Angular 17+
