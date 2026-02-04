# Guía de Implementación Frontend - Sistema de Estados de Pago

## Contexto para Claude

Necesito implementar en el frontend un sistema de gestión de estados de pago para estudiantes. El backend ya está implementado y necesito que modifiques el frontend para:

1. Mostrar el estado de pago en la **tabla de estudiantes**
2. Mostrar el estado de pago en la **lista de asistencia de estudiantes**
3. Permitir **cambiar el estado de pago manualmente**
4. Permitir **registrar pagos en efectivo**
5. Filtrar estudiantes por estado de pago

---

## 1. Estados de Pago Disponibles

```typescript
// Enum de estados de pago
enum EstadoPago {
  PENDIENTE = 'PENDIENTE',
  AL_DIA = 'AL_DIA',
  EN_MORA = 'EN_MORA',
  COMPROMISO_PAGO = 'COMPROMISO_PAGO'
}

// Configuración de colores y descripciones
const estadoPagoConfig = {
  PENDIENTE: {
    label: 'Pendiente por pagar',
    color: '#FFA726', // Naranja
    bgColor: '#FFF3E0',
    icon: 'clock' // o el icono que uses
  },
  AL_DIA: {
    label: 'Al día',
    color: '#66BB6A', // Verde
    bgColor: '#E8F5E9',
    icon: 'check-circle'
  },
  EN_MORA: {
    label: 'En mora',
    color: '#EF5350', // Rojo
    bgColor: '#FFEBEE',
    icon: 'alert-circle'
  },
  COMPROMISO_PAGO: {
    label: 'Compromiso de pago',
    color: '#42A5F5', // Azul
    bgColor: '#E3F2FD',
    icon: 'calendar'
  }
};
```

---

## 2. Interfaces/Types Necesarios

```typescript
// Interface para estudiante con estado de pago
interface EstudianteConEstadoPago {
  idEstudiante: number;
  nombreCompleto: string;
  numeroDocumento: string;
  tipoDocumento: string;
  edad: number;
  fotoUrl: string | null;
  celularEstudiante: string | null;
  correoEstudiante: string | null;
  estado: boolean;
  estadoPago: 'PENDIENTE' | 'AL_DIA' | 'EN_MORA' | 'COMPROMISO_PAGO';
  estadoPagoDescripcion: string;
  colorEstadoPago: string;
  idSede: number | null;
  nombreSede: string | null;
  nivelActual: string | null;
  fechaUltimoPago: string | null; // formato: "2026-01-15"
  mesUltimoPago: string | null;   // formato: "ENERO_2026"
}

// Interface para asistencia con estado de pago
interface AsistenciaEstudianteConEstadoPago {
  idAsistencia: number;
  fecha: string;
  asistio: boolean;
  observaciones: string | null;
  idEstudiante: number;
  nombreCompleto: string;
  numeroDocumento: string;
  fotoUrl: string | null;
  edad: number;
  estadoPago: 'PENDIENTE' | 'AL_DIA' | 'EN_MORA' | 'COMPROMISO_PAGO';
  estadoPagoDescripcion: string;
  colorEstadoPago: string;
  idEquipo: number | null;
  nombreEquipo: string | null;
}

// DTO para cambiar estado de pago
interface CambioEstadoPagoDTO {
  nuevoEstado: 'PENDIENTE' | 'AL_DIA' | 'EN_MORA' | 'COMPROMISO_PAGO';
  observacion?: string;
  mesPagado?: string; // formato: "FEBRERO_2026"
}

// DTO para registrar pago en efectivo
interface RegistroPagoEfectivoDTO {
  mesPagado: string;  // formato: "FEBRERO_2026"
  valor: number;
  observacion?: string;
  referenciaPago?: string;
}
```

---

## 3. Endpoints y Ejemplos

### 3.1 Obtener Estudiantes con Estado de Pago

#### Todos los estudiantes
```http
GET /api/estudiantes/con-estado-pago
```

**Response (200 OK):**
```json
[
  {
    "idEstudiante": 1,
    "nombreCompleto": "Juan Carlos Pérez López",
    "numeroDocumento": "1234567890",
    "tipoDocumento": "CC",
    "edad": 15,
    "fotoUrl": "/uploads/estudiantes/foto1.jpg",
    "celularEstudiante": "3001234567",
    "correoEstudiante": "juan@email.com",
    "estado": true,
    "estadoPago": "AL_DIA",
    "estadoPagoDescripcion": "Al día",
    "colorEstadoPago": "#66BB6A",
    "idSede": 1,
    "nombreSede": "Sede Principal",
    "nivelActual": "INTERMEDIO",
    "fechaUltimoPago": "2026-01-15",
    "mesUltimoPago": "ENERO_2026"
  },
  {
    "idEstudiante": 2,
    "nombreCompleto": "María García Rodríguez",
    "numeroDocumento": "0987654321",
    "tipoDocumento": "TI",
    "edad": 12,
    "fotoUrl": null,
    "celularEstudiante": "3009876543",
    "correoEstudiante": "maria@email.com",
    "estado": true,
    "estadoPago": "EN_MORA",
    "estadoPagoDescripcion": "En mora",
    "colorEstadoPago": "#EF5350",
    "idSede": 1,
    "nombreSede": "Sede Principal",
    "nivelActual": "INICIANTE",
    "fechaUltimoPago": "2025-12-10",
    "mesUltimoPago": "DICIEMBRE_2025"
  }
]
```

#### Solo estudiantes activos
```http
GET /api/estudiantes/activos/con-estado-pago
```

#### Filtrar por estado de pago específico
```http
GET /api/estudiantes/estado-pago/EN_MORA
GET /api/estudiantes/estado-pago/PENDIENTE
GET /api/estudiantes/estado-pago/AL_DIA
GET /api/estudiantes/estado-pago/COMPROMISO_PAGO
```

#### Endpoints directos de filtro
```http
GET /api/estudiantes/en-mora
GET /api/estudiantes/pendientes-pago
GET /api/estudiantes/al-dia
```

#### Por sede con estado de pago
```http
GET /api/estudiantes/sede/1/con-estado-pago
```

---

### 3.2 Cambiar Estado de Pago Manualmente

```http
PATCH /api/estudiantes/{id}/estado-pago
Content-Type: application/json
```

**Request Body:**
```json
{
  "nuevoEstado": "COMPROMISO_PAGO",
  "observacion": "Acordó pagar el próximo viernes",
  "mesPagado": "FEBRERO_2026"
}
```

**Response (200 OK):**
```json
{
  "message": "Estado de pago actualizado exitosamente",
  "estudiante": "Juan Carlos Pérez López",
  "nuevoEstado": "COMPROMISO_PAGO",
  "descripcion": "Compromiso de pago"
}
```

**Response (404 Not Found):**
```json
{
  "error": "Estudiante no encontrado con ID: 999"
}
```

---

### 3.3 Registrar Pago en Efectivo

```http
POST /api/estudiantes/{id}/pago-efectivo
Content-Type: application/json
```

**Request Body:**
```json
{
  "mesPagado": "FEBRERO_2026",
  "valor": 50000,
  "observacion": "Pago en efectivo recibido en secretaría",
  "referenciaPago": "REC-2026-001"
}
```

**Response (201 Created):**
```json
{
  "message": "Pago en efectivo registrado exitosamente",
  "idPago": 45,
  "mesPagado": "FEBRERO_2026",
  "valor": 50000,
  "referencia": "REC-2026-001",
  "nuevoEstadoEstudiante": "AL_DIA"
}
```

---

### 3.4 Asistencia de Estudiantes con Estado de Pago

#### Todas las asistencias
```http
GET /api/asistencia-estudiante/con-estado-pago
```

**Response (200 OK):**
```json
[
  {
    "idAsistencia": 1,
    "fecha": "2026-02-02",
    "asistio": true,
    "observaciones": null,
    "idEstudiante": 1,
    "nombreCompleto": "Juan Carlos Pérez López",
    "numeroDocumento": "1234567890",
    "fotoUrl": "/uploads/estudiantes/foto1.jpg",
    "edad": 15,
    "estadoPago": "AL_DIA",
    "estadoPagoDescripcion": "Al día",
    "colorEstadoPago": "#66BB6A",
    "idEquipo": 1,
    "nombreEquipo": "Sub-15 Masculino"
  },
  {
    "idAsistencia": 2,
    "fecha": "2026-02-02",
    "asistio": false,
    "observaciones": "Enfermo",
    "idEstudiante": 2,
    "nombreCompleto": "María García Rodríguez",
    "numeroDocumento": "0987654321",
    "fotoUrl": null,
    "edad": 12,
    "estadoPago": "EN_MORA",
    "estadoPagoDescripcion": "En mora",
    "colorEstadoPago": "#EF5350",
    "idEquipo": 2,
    "nombreEquipo": "Sub-12 Femenino"
  }
]
```

#### Por fecha específica
```http
GET /api/asistencia-estudiante/fecha/2026-02-02/con-estado-pago
```

#### Por equipo
```http
GET /api/asistencia-estudiante/equipo/1/con-estado-pago
```

#### Por equipo y fecha
```http
GET /api/asistencia-estudiante/equipo/1/fecha/2026-02-02/con-estado-pago
```

#### Por estudiante
```http
GET /api/asistencia-estudiante/estudiante/1/con-estado-pago
```

---

### 3.5 Verificar Estados de Pago (Admin)

```http
POST /api/estudiantes/verificar-estados-pago
```

**Response (200 OK):**
```json
{
  "message": "Verificación de estados de pago completada"
}
```

---

## 4. Código del Backend (Referencia)

### 4.1 Entidad Estudiante - Campo estadoPago

```java
// En Estudiante.java
@Enumerated(EnumType.STRING)
@Column(length = 20)
private EstadoPago estadoPago = EstadoPago.PENDIENTE;

public enum EstadoPago {
    PENDIENTE,
    AL_DIA,
    EN_MORA,
    COMPROMISO_PAGO
}
```

### 4.2 DTO EstudianteConEstadoPago

```java
@Data
@Builder
public class EstudianteConEstadoPagoDTO {
    private Integer idEstudiante;
    private String nombreCompleto;
    private String numeroDocumento;
    private String tipoDocumento;
    private Integer edad;
    private String fotoUrl;
    private String celularEstudiante;
    private String correoEstudiante;
    private Boolean estado;
    private Estudiante.EstadoPago estadoPago;
    private String estadoPagoDescripcion;
    private String colorEstadoPago;
    private Integer idSede;
    private String nombreSede;
    private String nivelActual;
    private LocalDate fechaUltimoPago;
    private String mesUltimoPago;
    
    public static String getDescripcionEstadoPago(Estudiante.EstadoPago estadoPago) {
        if (estadoPago == null) return "Sin estado";
        switch (estadoPago) {
            case PENDIENTE: return "Pendiente por pagar";
            case AL_DIA: return "Al día";
            case EN_MORA: return "En mora";
            case COMPROMISO_PAGO: return "Compromiso de pago";
            default: return "Desconocido";
        }
    }
    
    public static String getColorEstadoPago(Estudiante.EstadoPago estadoPago) {
        if (estadoPago == null) return "#9E9E9E";
        switch (estadoPago) {
            case PENDIENTE: return "#FFA726";
            case AL_DIA: return "#66BB6A";
            case EN_MORA: return "#EF5350";
            case COMPROMISO_PAGO: return "#42A5F5";
            default: return "#9E9E9E";
        }
    }
}
```

### 4.3 Servicio - Registrar Pago en Efectivo

```java
@Transactional
public Pago registrarPagoEfectivo(RegistroPagoEfectivoDTO pagoDTO) {
    Estudiante estudiante = estudianteRepository.findById(pagoDTO.getIdEstudiante())
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
    
    Pago pago = new Pago();
    pago.setEstudiante(estudiante);
    pago.setMesPagado(pagoDTO.getMesPagado());
    pago.setValor(pagoDTO.getValor());
    pago.setMetodoPago(Pago.MetodoPago.EFECTIVO);
    pago.setReferenciaPago(pagoDTO.getReferenciaPago());
    pago.setFechaPago(LocalDate.now());
    pago.setHoraPago(LocalTime.now());
    pago.setEstadoPago(Pago.EstadoPago.PAGADO);
    
    Pago pagoGuardado = pagoRepository.save(pago);
    
    // Actualizar estado del estudiante a AL_DIA automáticamente
    estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
    estudianteRepository.save(estudiante);
    
    return pagoGuardado;
}
```

---

## 5. Componentes Frontend a Crear/Modificar

### 5.1 Badge/Chip de Estado de Pago

Crear un componente reutilizable para mostrar el estado de pago:

```tsx
// Ejemplo de componente EstadoPagoBadge
interface EstadoPagoBadgeProps {
  estadoPago: 'PENDIENTE' | 'AL_DIA' | 'EN_MORA' | 'COMPROMISO_PAGO';
  showLabel?: boolean;
}

const EstadoPagoBadge: React.FC<EstadoPagoBadgeProps> = ({ estadoPago, showLabel = true }) => {
  const config = {
    PENDIENTE: { label: 'Pendiente', color: '#FFA726', bg: '#FFF3E0' },
    AL_DIA: { label: 'Al día', color: '#66BB6A', bg: '#E8F5E9' },
    EN_MORA: { label: 'En mora', color: '#EF5350', bg: '#FFEBEE' },
    COMPROMISO_PAGO: { label: 'Compromiso', color: '#42A5F5', bg: '#E3F2FD' }
  };
  
  const { label, color, bg } = config[estadoPago] || config.PENDIENTE;
  
  return (
    <span style={{ 
      backgroundColor: bg, 
      color: color, 
      padding: '4px 8px', 
      borderRadius: '4px',
      fontSize: '12px',
      fontWeight: 'bold'
    }}>
      {showLabel ? label : '●'}
    </span>
  );
};
```

### 5.2 Modificar Tabla de Estudiantes

Agregar columna de estado de pago a la tabla existente:

```tsx
// Columna adicional para la tabla de estudiantes
{
  header: 'Estado Pago',
  accessorKey: 'estadoPago',
  cell: ({ row }) => (
    <EstadoPagoBadge estadoPago={row.original.estadoPago} />
  )
}
```

### 5.3 Filtros por Estado de Pago

```tsx
// Selector de filtro
<Select
  value={filtroEstadoPago}
  onChange={(e) => setFiltroEstadoPago(e.target.value)}
>
  <option value="">Todos los estados</option>
  <option value="AL_DIA">Al día</option>
  <option value="PENDIENTE">Pendientes</option>
  <option value="EN_MORA">En mora</option>
  <option value="COMPROMISO_PAGO">Compromiso de pago</option>
</Select>
```

### 5.4 Modal para Cambiar Estado de Pago

```tsx
// Modal para cambiar estado manualmente
interface CambiarEstadoModalProps {
  isOpen: boolean;
  onClose: () => void;
  estudiante: EstudianteConEstadoPago;
  onSuccess: () => void;
}

const CambiarEstadoModal: React.FC<CambiarEstadoModalProps> = ({ 
  isOpen, onClose, estudiante, onSuccess 
}) => {
  const [nuevoEstado, setNuevoEstado] = useState(estudiante.estadoPago);
  const [observacion, setObservacion] = useState('');
  
  const handleSubmit = async () => {
    try {
      await axios.patch(`/api/estudiantes/${estudiante.idEstudiante}/estado-pago`, {
        nuevoEstado,
        observacion
      });
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Error al cambiar estado:', error);
    }
  };
  
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <h2>Cambiar Estado de Pago</h2>
      <p>Estudiante: {estudiante.nombreCompleto}</p>
      
      <Select value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value)}>
        <option value="PENDIENTE">Pendiente por pagar</option>
        <option value="AL_DIA">Al día</option>
        <option value="EN_MORA">En mora</option>
        <option value="COMPROMISO_PAGO">Compromiso de pago</option>
      </Select>
      
      <Textarea
        placeholder="Observación (opcional)"
        value={observacion}
        onChange={(e) => setObservacion(e.target.value)}
      />
      
      <Button onClick={handleSubmit}>Guardar</Button>
    </Modal>
  );
};
```

### 5.5 Modal para Registrar Pago en Efectivo

```tsx
interface PagoEfectivoModalProps {
  isOpen: boolean;
  onClose: () => void;
  estudiante: EstudianteConEstadoPago;
  onSuccess: () => void;
}

const PagoEfectivoModal: React.FC<PagoEfectivoModalProps> = ({
  isOpen, onClose, estudiante, onSuccess
}) => {
  const [mesPagado, setMesPagado] = useState('');
  const [valor, setValor] = useState(50000); // Valor por defecto
  const [referencia, setReferencia] = useState('');
  const [observacion, setObservacion] = useState('');
  
  // Generar opciones de meses
  const mesesOptions = [
    'ENERO_2026', 'FEBRERO_2026', 'MARZO_2026', 'ABRIL_2026',
    'MAYO_2026', 'JUNIO_2026', 'JULIO_2026', 'AGOSTO_2026',
    'SEPTIEMBRE_2026', 'OCTUBRE_2026', 'NOVIEMBRE_2026', 'DICIEMBRE_2026'
  ];
  
  const handleSubmit = async () => {
    try {
      await axios.post(`/api/estudiantes/${estudiante.idEstudiante}/pago-efectivo`, {
        mesPagado,
        valor,
        observacion,
        referenciaPago: referencia
      });
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Error al registrar pago:', error);
    }
  };
  
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <h2>Registrar Pago en Efectivo</h2>
      <p>Estudiante: {estudiante.nombreCompleto}</p>
      
      <Select value={mesPagado} onChange={(e) => setMesPagado(e.target.value)}>
        <option value="">Seleccione el mes</option>
        {mesesOptions.map(mes => (
          <option key={mes} value={mes}>{mes.replace('_', ' ')}</option>
        ))}
      </Select>
      
      <Input
        type="number"
        label="Valor del pago"
        value={valor}
        onChange={(e) => setValor(Number(e.target.value))}
      />
      
      <Input
        label="Número de recibo/referencia"
        value={referencia}
        onChange={(e) => setReferencia(e.target.value)}
      />
      
      <Textarea
        placeholder="Observación (opcional)"
        value={observacion}
        onChange={(e) => setObservacion(e.target.value)}
      />
      
      <Button onClick={handleSubmit}>Registrar Pago</Button>
    </Modal>
  );
};
```

---

## 6. Servicios API (Frontend)

```typescript
// services/estudianteService.ts

const API_URL = '/api/estudiantes';

export const estudianteService = {
  // Obtener todos con estado de pago
  getAllConEstadoPago: async (): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/con-estado-pago`);
    return response.data;
  },
  
  // Obtener activos con estado de pago
  getActivosConEstadoPago: async (): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/activos/con-estado-pago`);
    return response.data;
  },
  
  // Filtrar por estado de pago
  getByEstadoPago: async (estado: string): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/estado-pago/${estado}`);
    return response.data;
  },
  
  // Obtener en mora
  getEnMora: async (): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/en-mora`);
    return response.data;
  },
  
  // Obtener pendientes
  getPendientes: async (): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/pendientes-pago`);
    return response.data;
  },
  
  // Obtener al día
  getAlDia: async (): Promise<EstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL}/al-dia`);
    return response.data;
  },
  
  // Cambiar estado de pago
  cambiarEstadoPago: async (id: number, data: CambioEstadoPagoDTO): Promise<any> => {
    const response = await axios.patch(`${API_URL}/${id}/estado-pago`, data);
    return response.data;
  },
  
  // Registrar pago en efectivo
  registrarPagoEfectivo: async (id: number, data: RegistroPagoEfectivoDTO): Promise<any> => {
    const response = await axios.post(`${API_URL}/${id}/pago-efectivo`, data);
    return response.data;
  }
};

// services/asistenciaEstudianteService.ts

const API_URL_ASISTENCIA = '/api/asistencia-estudiante';

export const asistenciaEstudianteService = {
  // Obtener todas con estado de pago
  getAllConEstadoPago: async (): Promise<AsistenciaEstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL_ASISTENCIA}/con-estado-pago`);
    return response.data;
  },
  
  // Por fecha
  getByFechaConEstadoPago: async (fecha: string): Promise<AsistenciaEstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL_ASISTENCIA}/fecha/${fecha}/con-estado-pago`);
    return response.data;
  },
  
  // Por equipo
  getByEquipoConEstadoPago: async (idEquipo: number): Promise<AsistenciaEstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL_ASISTENCIA}/equipo/${idEquipo}/con-estado-pago`);
    return response.data;
  },
  
  // Por equipo y fecha
  getByEquipoYFechaConEstadoPago: async (idEquipo: number, fecha: string): Promise<AsistenciaEstudianteConEstadoPago[]> => {
    const response = await axios.get(`${API_URL_ASISTENCIA}/equipo/${idEquipo}/fecha/${fecha}/con-estado-pago`);
    return response.data;
  }
};
```

---

## 7. Lista de Tareas para el Frontend

### Tabla de Estudiantes
- [ ] Agregar columna "Estado de Pago" con badge de color
- [ ] Agregar filtro/dropdown para filtrar por estado de pago
- [ ] Agregar botón de acción "Cambiar Estado" en cada fila
- [ ] Agregar botón de acción "Registrar Pago" en cada fila
- [ ] Implementar modal para cambiar estado de pago
- [ ] Implementar modal para registrar pago en efectivo
- [ ] Cambiar el endpoint de obtener estudiantes a `/api/estudiantes/con-estado-pago`

### Lista de Asistencia
- [ ] Agregar columna/indicador de estado de pago junto al nombre del estudiante
- [ ] Mostrar badge de color según el estado
- [ ] Cambiar el endpoint para usar `/api/asistencia-estudiante/con-estado-pago`
- [ ] Opcionalmente: resaltar filas de estudiantes en mora

### Dashboard/Estadísticas (Opcional)
- [ ] Mostrar contadores: cuántos al día, cuántos en mora, cuántos pendientes
- [ ] Gráfico de distribución de estados de pago

---

## 8. Notas Importantes

1. **El estado se actualiza automáticamente con pagos online (Wompi)** - No requiere acción del frontend
2. **Para pagos en efectivo** - Usar el endpoint `/api/estudiantes/{id}/pago-efectivo`
3. **Formato de mes** - Siempre usar formato `MES_AÑO` (ej: `FEBRERO_2026`)
4. **Colores son enviados desde el backend** - Se puede usar `colorEstadoPago` directamente
5. **La descripción también viene del backend** - Usar `estadoPagoDescripcion`

---

## 9. Pruebas con cURL

### Obtener estudiantes con estado de pago
```bash
curl -X GET http://localhost:8080/api/estudiantes/con-estado-pago \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Cambiar estado a compromiso de pago
```bash
curl -X PATCH http://localhost:8080/api/estudiantes/1/estado-pago \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "nuevoEstado": "COMPROMISO_PAGO",
    "observacion": "Acordó pagar el viernes"
  }'
```

### Registrar pago en efectivo
```bash
curl -X POST http://localhost:8080/api/estudiantes/1/pago-efectivo \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "mesPagado": "FEBRERO_2026",
    "valor": 50000,
    "observacion": "Pago en efectivo",
    "referenciaPago": "REC-001"
  }'
```

### Obtener asistencias con estado de pago
```bash
curl -X GET http://localhost:8080/api/asistencia-estudiante/con-estado-pago \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Obtener asistencias por equipo y fecha
```bash
curl -X GET "http://localhost:8080/api/asistencia-estudiante/equipo/1/fecha/2026-02-02/con-estado-pago" \
  -H "Authorization: Bearer YOUR_TOKEN"
```
