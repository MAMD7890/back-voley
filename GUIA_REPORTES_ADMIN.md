# 📊 Guía de Reportes de Pagos para Panel de Administrador

## Descripción General

Este documento describe los endpoints disponibles para visualizar reportes de pagos realizados mediante Wompi en el panel de administrador. Cada pago está asociado a un estudiante y cuando se realiza un pago exitoso, el estado del estudiante se actualiza automáticamente.

---

## 🔗 Endpoints Disponibles

### 1. Resumen General de Pagos (Dashboard)

**Endpoint:** `GET /api/pagos/reportes/resumen`

**Descripción:** Obtiene estadísticas completas para el dashboard del administrador.

**Respuesta:**
```json
{
  "totalPagos": 150,
  "pagosAprobados": 120,
  "pagosPendientes": 20,
  "pagosVencidos": 10,
  "montoTotalRecaudado": 12000000.00,
  "montoPendiente": 2000000.00,
  "montoPromedioTransaccion": 100000.00,
  "pagosOnline": 80,
  "pagosEfectivo": 40,
  "montoOnline": 8000000.00,
  "montoEfectivo": 4000000.00,
  "estudiantesAlDia": 45,
  "estudiantesEnMora": 10,
  "estudiantesPendientes": 15,
  "estudiantesConCompromiso": 5,
  "ultimosPagos": [...],
  "pagosPorMes": [
    { "mes": "Enero 2024", "cantidad": 25, "monto": 2500000.00 },
    { "mes": "Febrero 2024", "cantidad": 30, "monto": 3000000.00 }
  ]
}
```

---

### 2. Listado de Todos los Pagos con Información del Estudiante

**Endpoint:** `GET /api/pagos/reportes/wompi`

**Descripción:** Obtiene todos los pagos con la información del estudiante asociado.

**Respuesta:**
```json
[
  {
    "idPago": 1,
    "referenciaPago": "REF-1234567890",
    "wompiTransactionId": "12345-abcde-67890",
    "monto": 100000.00,
    "mesPagado": "Enero 2024",
    "fechaPago": "2024-01-15",
    "horaPago": "14:30:00",
    "estadoPago": "PAGADO",
    "metodoPago": "ONLINE",
    "idEstudiante": 5,
    "nombreEstudiante": "Juan Pérez García",
    "emailEstudiante": "juan@email.com",
    "telefonoEstudiante": "3001234567",
    "estadoPagoEstudiante": "AL_DIA",
    "colorEstadoPago": "#28a745",
    "descripcionEstadoPago": "Al día con los pagos"
  }
]
```

---

### 3. Pagos Paginados

**Endpoint:** `GET /api/pagos/reportes/wompi/paginado?page=0&size=10`

**Parámetros:**
- `page`: Número de página (desde 0)
- `size`: Cantidad de registros por página

**Descripción:** Ideal para tablas con paginación en el frontend.

**Respuesta:**
```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 15,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

---

### 4. Solo Pagos Online (Wompi)

**Endpoint:** `GET /api/pagos/reportes/online`

**Descripción:** Filtra solo los pagos realizados a través de Wompi (online).

---

### 5. Pagos por Rango de Fechas

**Endpoint:** `GET /api/pagos/reportes/fecha?desde=2024-01-01&hasta=2024-01-31`

**Parámetros:**
- `desde`: Fecha inicial (formato: YYYY-MM-DD)
- `hasta`: Fecha final (formato: YYYY-MM-DD)

**Descripción:** Filtra pagos dentro de un período específico.

---

### 6. Pagos de un Estudiante Específico

**Endpoint:** `GET /api/pagos/reportes/estudiante/{idEstudiante}`

**Descripción:** Obtiene el historial de pagos de un estudiante en particular.

---

## 🎨 Colores de Estado del Estudiante

| Estado | Color | Código Hex |
|--------|-------|------------|
| AL_DIA | Verde | `#28a745` |
| EN_MORA | Rojo | `#dc3545` |
| COMPROMISO_PAGO | Amarillo | `#ffc107` |
| PENDIENTE | Gris | `#6c757d` |

---

## 🔄 Flujo Automático de Actualización de Estado

1. **Estudiante realiza pago en Wompi**
2. **Wompi envía webhook** al backend (`POST /api/wompi/webhook`)
3. **Backend procesa el pago:**
   - Actualiza el estado del pago a `PAGADO`
   - Actualiza el estado del estudiante a `AL_DIA`
4. **Frontend puede consultar** los reportes actualizados

---

## 📱 Ejemplo de Implementación en Frontend (React/Angular)

### React con Fetch

```javascript
// Obtener resumen para dashboard
const obtenerResumen = async () => {
  const response = await fetch('/api/pagos/reportes/resumen', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data;
};

// Obtener pagos con paginación
const obtenerPagosPaginados = async (page = 0, size = 10) => {
  const response = await fetch(
    `/api/pagos/reportes/wompi/paginado?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return await response.json();
};

// Filtrar por fechas
const filtrarPorFechas = async (desde, hasta) => {
  const response = await fetch(
    `/api/pagos/reportes/fecha?desde=${desde}&hasta=${hasta}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return await response.json();
};
```

### Angular con HttpClient

```typescript
// payment-report.service.ts
@Injectable({ providedIn: 'root' })
export class PaymentReportService {
  private apiUrl = '/api/pagos/reportes';

  constructor(private http: HttpClient) {}

  getResumen(): Observable<ResumenPagosWompiDTO> {
    return this.http.get<ResumenPagosWompiDTO>(`${this.apiUrl}/resumen`);
  }

  getPagosPaginados(page: number, size: number): Observable<Page<ReportePagoWompiDTO>> {
    return this.http.get<Page<ReportePagoWompiDTO>>(
      `${this.apiUrl}/wompi/paginado?page=${page}&size=${size}`
    );
  }

  getPagosOnline(): Observable<ReportePagoWompiDTO[]> {
    return this.http.get<ReportePagoWompiDTO[]>(`${this.apiUrl}/online`);
  }

  getPagosPorFechas(desde: string, hasta: string): Observable<ReportePagoWompiDTO[]> {
    return this.http.get<ReportePagoWompiDTO[]>(
      `${this.apiUrl}/fecha?desde=${desde}&hasta=${hasta}`
    );
  }

  getPagosEstudiante(idEstudiante: number): Observable<ReportePagoWompiDTO[]> {
    return this.http.get<ReportePagoWompiDTO[]>(`${this.apiUrl}/estudiante/${idEstudiante}`);
  }
}
```

---

## 📊 Componente de Tabla con Colores (React)

```jsx
const TablaReportes = ({ pagos }) => {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Estudiante</th>
          <th>Monto</th>
          <th>Fecha</th>
          <th>Estado Estudiante</th>
        </tr>
      </thead>
      <tbody>
        {pagos.map(pago => (
          <tr key={pago.idPago}>
            <td>{pago.idPago}</td>
            <td>{pago.nombreEstudiante}</td>
            <td>${pago.monto.toLocaleString()}</td>
            <td>{pago.fechaPago}</td>
            <td>
              <span 
                className="badge" 
                style={{ backgroundColor: pago.colorEstadoPago }}
              >
                {pago.descripcionEstadoPago}
              </span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
```

---

## 📈 Datos para Gráficos

El campo `pagosPorMes` del resumen puede usarse directamente con librerías como Chart.js, Recharts o ApexCharts:

```javascript
// Configuración para Chart.js
const chartData = {
  labels: resumen.pagosPorMes.map(p => p.mes),
  datasets: [{
    label: 'Monto Recaudado',
    data: resumen.pagosPorMes.map(p => p.monto),
    backgroundColor: '#28a745'
  }]
};
```

---

## ✅ Resumen de Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/pagos/reportes/resumen` | Dashboard con estadísticas |
| GET | `/api/pagos/reportes/wompi` | Todos los pagos |
| GET | `/api/pagos/reportes/wompi/paginado` | Pagos paginados |
| GET | `/api/pagos/reportes/online` | Solo pagos online |
| GET | `/api/pagos/reportes/fecha` | Filtro por fechas |
| GET | `/api/pagos/reportes/estudiante/{id}` | Pagos de un estudiante |

---

## 🔐 Autenticación

Todos los endpoints requieren un token JWT válido en el header:

```
Authorization: Bearer <token>
```

Excepto los endpoints de Wompi que son públicos para recibir webhooks.
