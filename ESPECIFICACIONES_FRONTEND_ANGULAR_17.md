# ESPECIFICACIONES FRONTEND - IMPORTACIÓN DE ESTUDIANTES DESDE EXCEL (Angular 17)

## Versión: 1.0
## Fecha: 16 de Febrero de 2026
## Framework: Angular 17
## Estado: PRODUCCIÓN

---

## 📋 TABLA DE CONTENIDOS

1. [Instalación de Dependencias](#instalación-de-dependencias)
2. [Modelos e Interfaces](#modelos-e-interfaces)
3. [Servicios](#servicios)
4. [Componentes](#componentes)
5. [Rutas](#rutas)
6. [Validaciones](#validaciones)
7. [Manejo de Errores](#manejo-de-errores)
8. [Ejemplo Completo](#ejemplo-completo)
9. [Testing](#testing)
10. [Buenas Prácticas](#buenas-prácticas)

---

## 🔧 INSTALACIÓN DE DEPENDENCIAS

### 1. Dependencias Necesarias

Las siguientes dependencias ya deben estar en tu `package.json`:

```json
{
  "dependencies": {
    "@angular/common": "^17.0.0",
    "@angular/core": "^17.0.0",
    "@angular/forms": "^17.0.0",
    "@angular/http": "^17.0.0",
    "@angular/platform-browser": "^17.0.0",
    "rxjs": "^7.8.0"
  },
  "devDependencies": {
    "@angular/cli": "^17.0.0",
    "typescript": "^5.2.0"
  }
}
```

### 2. Librerías Adicionales Opcionales (pero recomendadas)

Para mejora de UX al cargar archivos:

```bash
npm install ngx-file-drop --save
# O
npm install ng2-file-upload --save
```

Para notificaciones:

```bash
npm install ngx-toastr --save
npm install @ngx-translate/core --save
```

Para tablas de datos (opcional):

```bash
npm install ngx-datatable --save
```

---

## 📊 MODELOS E INTERFACES

### 1. Crear archivo: `src/app/models/excel-import.model.ts`

```typescript
/**
 * DTO para mapear datos del Excel de inscripción de estudiantes
 */
export interface ExcelEstudianteImportDTO {
  // Información personal del estudiante
  nombreCompleto: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: Date;
  edad: number;
  sexo: string;

  // Información de contacto estudiante
  direccionResidencia: string;
  barrio: string;
  celularEstudiante: string;
  whatsappEstudiante: string;
  correoEstudiante: string;

  // Información de la sede
  nombreSede: string;

  // Información del tutor/padre
  nombreTutor: string;
  parentescoTutor: string;
  documentoTutor: string;
  telefonoTutor: string;
  correoTutor: string;
  ocupacionTutor: string;

  // Información académica
  institucionEducativa: string;
  jornada: string;
  gradoActual: number;

  // Información médica
  eps: string;
  tipoSangre: string;
  alergias: string;
  enfermedadesCondiciones: string;
  medicamentos: string;
  certificadoMedicoDeportivo: boolean;

  // Información de pagos
  diaPagoMes: number;

  // Información contacto emergencia
  nombreEmergencia: string;
  telefonoEmergencia: string;
  parentescoEmergencia: string;
  ocupacionEmergencia: string;
  correoEmergencia: string;

  // Información sobre poblaciones vulnerables
  perteneceIgbtiq: boolean;
  personaDiscapacidad: boolean;
  condicionDiscapacidad: string;
  migranteRefugiado: boolean;
  poblacionEtnica: string;
  religion: string;

  // Información deportiva
  experienciaVoleibol: string;
  otrasDisciplinas: string;
  posicionPreferida: string;
  dominancia: string;
  nivelActual: string;
  clubesAnteriores: string;

  // Consentimiento informado
  consentimientoInformado: boolean;
  firmaDigital: string;
  fechaDiligenciamiento: Date;

  // Información para validación y reporte
  numeroFila: number;
  erroresValidacion: string;
  procesadoExitosamente: boolean;
}

/**
 * DTO para la respuesta de importación de estudiantes desde Excel
 */
export interface ExcelImportResponseDTO {
  exitosos: number;
  errores: number;
  total: number;
  resultados: ExcelImportResultado[];
}

/**
 * Resultado de importación por estudiante
 */
export interface ExcelImportResultado {
  fila: number;
  nombre: string;
  estado: 'EXITOSO' | 'ERROR';
  mensaje: string;
  idEstudiante?: number;
  email?: string;
  password?: string;
}

/**
 * Respuesta de error del servidor
 */
export interface ErrorResponse {
  error: string;
  detalles?: string;
}

/**
 * Estado para componente de importación
 */
export interface ImportacionState {
  cargando: boolean;
  archivoSeleccionado: File | null;
  sedeId: number | null;
  respuesta: ExcelImportResponseDTO | null;
  error: ErrorResponse | null;
  progreso: number;
}
```

---

## 🔌 SERVICIOS

### 1. Crear archivo: `src/app/services/excel-import.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpProgressEvent, HttpResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, finalize, tap } from 'rxjs/operators';
import { 
  ExcelImportResponseDTO, 
  ErrorResponse,
  ExcelImportResultado 
} from '../models/excel-import.model';

@Injectable({
  providedIn: 'root'
})
export class ExcelImportService {

  private apiUrl = 'http://localhost:8080/api/estudiantes/importar-excel';
  
  // Sujeto para rastrear el progreso de carga
  private progresoSubject = new BehaviorSubject<number>(0);
  public progreso$ = this.progresoSubject.asObservable();

  // Sujeto para estado de carga
  private cargandoSubject = new BehaviorSubject<boolean>(false);
  public cargando$ = this.cargandoSubject.asObservable();

  constructor(private http: HttpClient) { }

  /**
   * Importa estudiantes desde un archivo Excel
   * @param archivo Archivo Excel (.xlsx)
   * @param sedeId ID de la sede
   * @returns Observable con la respuesta de importación
   */
  importarEstudiantesDesdeExcel(
    archivo: File,
    sedeId: number
  ): Observable<ExcelImportResponseDTO> {
    
    // Validaciones del lado del cliente
    if (!archivo) {
      return throwError(() => ({
        error: 'Archivo no seleccionado',
        detalles: 'Por favor selecciona un archivo Excel'
      } as ErrorResponse));
    }

    if (!this.esArchivoExcel(archivo)) {
      return throwError(() => ({
        error: 'Formato inválido',
        detalles: 'El archivo debe ser de tipo .xlsx (Excel 2007+)'
      } as ErrorResponse));
    }

    if (!sedeId || sedeId <= 0) {
      return throwError(() => ({
        error: 'Sede no seleccionada',
        detalles: 'Por favor selecciona una sede válida'
      } as ErrorResponse));
    }

    // Crear FormData
    const formData = new FormData();
    formData.append('file', archivo);

    // Marcar como cargando
    this.cargandoSubject.next(true);
    this.progresoSubject.next(0);

    // Realizar petición
    return this.http.post<ExcelImportResponseDTO>(
      `${this.apiUrl}?sedeId=${sedeId}`,
      formData,
      {
        reportProgress: true,
        observe: 'events'
      }
    ).pipe(
      tap((evento: HttpEvent<ExcelImportResponseDTO>) => {
        if (evento instanceof HttpProgressEvent) {
          // Actualizar progreso
          if (evento.total) {
            const progreso = Math.round((evento.loaded / evento.total) * 100);
            this.progresoSubject.next(progreso);
          }
        }
        if (evento instanceof HttpResponse) {
          // Completado
          this.progresoSubject.next(100);
        }
      }),
      finalize(() => {
        this.cargandoSubject.next(false);
        setTimeout(() => this.progresoSubject.next(0), 500);
      }),
      catchError((error: HttpErrorResponse) => {
        return throwError(() => this.manejarError(error));
      })
    ) as Observable<ExcelImportResponseDTO>;
  }

  /**
   * Verifica si el archivo es un Excel válido
   */
  private esArchivoExcel(archivo: File): boolean {
    const tiposValidos = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel'
    ];
    const extensionValida = archivo.name.toLowerCase().endsWith('.xlsx');
    const tipoValido = tiposValidos.includes(archivo.type);
    
    return extensionValida && tipoValido;
  }

  /**
   * Maneja errores de la respuesta del servidor
   */
  private manejarError(error: HttpErrorResponse): ErrorResponse {
    let mensajeError: ErrorResponse = {
      error: 'Error desconocido',
      detalles: 'Ocurrió un error al procesar la importación'
    };

    if (error.error instanceof ErrorEvent) {
      // Error del lado del cliente
      mensajeError = {
        error: error.error.message,
        detalles: 'Error de conexión'
      };
    } else {
      // Error del servidor
      mensajeError = {
        error: error.error?.error || 'Error en el servidor',
        detalles: error.error?.detalles || `HTTP ${error.status}`
      };
    }

    console.error('Error de importación:', mensajeError);
    return mensajeError;
  }

  /**
   * Retorna el progreso actual
   */
  obtenerProgreso(): number {
    return this.progresoSubject.value;
  }

  /**
   * Retorna si está cargando
   */
  estaCargando(): boolean {
    return this.cargandoSubject.value;
  }

  /**
   * Calcula estadísticas de la importación
   */
  calcularEstadisticas(respuesta: ExcelImportResponseDTO): {
    tasaExito: number;
    tasaError: number;
    exitosos: number;
    errores: number;
  } {
    const tasaExito = respuesta.total > 0 
      ? Math.round((respuesta.exitosos / respuesta.total) * 100) 
      : 0;
    const tasaError = 100 - tasaExito;

    return {
      tasaExito,
      tasaError,
      exitosos: respuesta.exitosos,
      errores: respuesta.errores
    };
  }

  /**
   * Genera un reporte CSV de la importación
   */
  generarReporteCsv(respuesta: ExcelImportResponseDTO): string {
    let csv = 'Fila,Nombre,Estado,Mensaje,Email,Contraseña\n';
    
    respuesta.resultados.forEach(resultado => {
      const fila = [
        resultado.fila,
        `"${resultado.nombre}"`,
        resultado.estado,
        `"${resultado.mensaje}"`,
        resultado.email || '',
        resultado.password || ''
      ].join(',');
      csv += fila + '\n';
    });

    return csv;
  }

  /**
   * Descarga el reporte como archivo
   */
  descargarReporte(respuesta: ExcelImportResponseDTO, nombreArchivo: string = 'reporte-importacion.csv'): void {
    const csv = this.generarReporteCsv(respuesta);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', nombreArchivo);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
```

### 2. Crear archivo: `src/app/services/sede.service.ts` (si no existe)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Sede {
  idSede: number;
  nombre: string;
  direccion?: string;
  telefono?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SedeService {

  private apiUrl = 'http://localhost:8080/api/sedes';

  constructor(private http: HttpClient) { }

  /**
   * Obtiene todas las sedes
   */
  obtenerSedes(): Observable<Sede[]> {
    return this.http.get<Sede[]>(this.apiUrl);
  }

  /**
   * Obtiene una sede por ID
   */
  obtenerSedePorId(id: number): Observable<Sede> {
    return this.http.get<Sede>(`${this.apiUrl}/${id}`);
  }
}
```

---

## 🎨 COMPONENTES

### 1. Crear archivo: `src/app/components/importar-estudiantes/importar-estudiantes.component.ts`

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ExcelImportService } from '../../services/excel-import.service';
import { SedeService, Sede } from '../../services/sede.service';
import { ExcelImportResponseDTO, ErrorResponse } from '../../models/excel-import.model';

@Component({
  selector: 'app-importar-estudiantes',
  templateUrl: './importar-estudiantes.component.html',
  styleUrls: ['./importar-estudiantes.component.scss']
})
export class ImportarEstudiantesComponent implements OnInit, OnDestroy {

  // Formulario
  formularioImportacion: FormGroup;

  // Estado
  archivoSeleccionado: File | null = null;
  cargando = false;
  progreso = 0;
  respuestaImportacion: ExcelImportResponseDTO | null = null;
  error: ErrorResponse | null = null;
  mostrarResultados = false;
  sedesDisponibles: Sede[] = [];
  sedeSeleccionada: number | null = null;

  // Para gestionar suscripciones
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private excelImportService: ExcelImportService,
    private sedeService: SedeService
  ) {
    this.formularioImportacion = this.crearFormulario();
  }

  ngOnInit(): void {
    this.cargarSedes();
    this.suscribirAlProgreso();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Crea el formulario reactivo
   */
  private crearFormulario(): FormGroup {
    return this.fb.group({
      sede: ['', [Validators.required]],
      archivo: ['', [Validators.required]]
    });
  }

  /**
   * Carga las sedes disponibles
   */
  private cargarSedes(): void {
    this.sedeService.obtenerSedes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (sedes) => {
          this.sedesDisponibles = sedes;
        },
        error: (error) => {
          console.error('Error cargando sedes:', error);
          this.mostrarError({
            error: 'Error al cargar sedes',
            detalles: 'No se pudieron cargar las sedes disponibles'
          });
        }
      });
  }

  /**
   * Se suscribe al progreso de carga
   */
  private suscribirAlProgreso(): void {
    this.excelImportService.progreso$
      .pipe(takeUntil(this.destroy$))
      .subscribe(progreso => {
        this.progreso = progreso;
      });

    this.excelImportService.cargando$
      .pipe(takeUntil(this.destroy$))
      .subscribe(cargando => {
        this.cargando = cargando;
      });
  }

  /**
   * Maneja la selección de archivo
   */
  onArchivoSeleccionado(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivos = input.files;

    if (!archivos || archivos.length === 0) {
      this.archivoSeleccionado = null;
      this.formularioImportacion.patchValue({ archivo: '' });
      return;
    }

    this.archivoSeleccionado = archivos[0];
    this.formularioImportacion.patchValue({ archivo: this.archivoSeleccionado.name });

    // Validación adicional del lado del cliente
    if (!this.archivoSeleccionado.name.toLowerCase().endsWith('.xlsx')) {
      this.mostrarError({
        error: 'Archivo inválido',
        detalles: 'El archivo debe ser de tipo .xlsx'
      });
      this.archivoSeleccionado = null;
    }
  }

  /**
   * Maneja la selección de sede
   */
  onSedeSeleccionada(sedeId: string): void {
    this.sedeSeleccionada = parseInt(sedeId, 10);
  }

  /**
   * Inicia la importación
   */
  iniciarImportacion(): void {
    // Validar formulario
    if (!this.formularioImportacion.valid) {
      this.mostrarError({
        error: 'Formulario incompleto',
        detalles: 'Por favor completa todos los campos requeridos'
      });
      return;
    }

    // Validar archivo
    if (!this.archivoSeleccionado) {
      this.mostrarError({
        error: 'Archivo no seleccionado',
        detalles: 'Por favor selecciona un archivo Excel'
      });
      return;
    }

    // Validar sede
    if (!this.sedeSeleccionada || this.sedeSeleccionada <= 0) {
      this.mostrarError({
        error: 'Sede no válida',
        detalles: 'Por favor selecciona una sede válida'
      });
      return;
    }

    // Limpiar errores previos
    this.error = null;

    // Realizar importación
    this.excelImportService.importarEstudiantesDesdeExcel(
      this.archivoSeleccionado,
      this.sedeSeleccionada
    ).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (respuesta) => {
          this.respuestaImportacion = respuesta;
          this.mostrarResultados = true;
          this.mostrarMensajeExito(respuesta);
        },
        error: (error: ErrorResponse) => {
          this.mostrarError(error);
        }
      });
  }

  /**
   * Descarga el reporte de importación
   */
  descargarReporte(): void {
    if (!this.respuestaImportacion) {
      return;
    }
    const fecha = new Date().toISOString().split('T')[0];
    this.excelImportService.descargarReporte(
      this.respuestaImportacion,
      `importacion-estudiantes-${fecha}.csv`
    );
  }

  /**
   * Reinicia el formulario
   */
  reiniciar(): void {
    this.formularioImportacion.reset();
    this.archivoSeleccionado = null;
    this.respuestaImportacion = null;
    this.error = null;
    this.mostrarResultados = false;
    this.progreso = 0;
    this.sedeSeleccionada = null;
  }

  /**
   * Muestra mensaje de error
   */
  private mostrarError(error: ErrorResponse): void {
    this.error = error;
    console.error('Error:', error);
    // Aquí también puedes integrar un servicio de notificaciones
    // this.toastr.error(error.detalles || error.error, 'Error');
  }

  /**
   * Muestra mensaje de éxito
   */
  private mostrarMensajeExito(respuesta: ExcelImportResponseDTO): void {
    const mensaje = `Se importaron ${respuesta.exitosos} estudiantes correctamente.`;
    const detalles = respuesta.errores > 0 
      ? ` ${respuesta.errores} registros fallaron.` 
      : '';
    console.log(mensaje + detalles);
    // Aquí puedes integrar un servicio de notificaciones
    // this.toastr.success(mensaje + detalles, 'Importación Completada');
  }

  /**
   * Obtiene el estado de un resultado
   */
  obtenerClaseEstado(resultado: any): string {
    return resultado.estado === 'EXITOSO' ? 'exitoso' : 'error';
  }

  /**
   * Obtiene el icono de un resultado
   */
  obtenerIconoEstado(resultado: any): string {
    return resultado.estado === 'EXITOSO' ? '✓' : '✗';
  }

  /**
   * Obtiene estadísticas de la importación
   */
  obtenerEstadisticas(): any {
    if (!this.respuestaImportacion) {
      return null;
    }
    return this.excelImportService.calcularEstadisticas(this.respuestaImportacion);
  }

  /**
   * Obtiene el nombre de la sede por ID
   */
  obtenerNombreSede(sedeId: number): string {
    const sede = this.sedesDisponibles.find(s => s.idSede === sedeId);
    return sede ? sede.nombre : 'Desconocida';
  }
}
```

### 2. Crear archivo: `src/app/components/importar-estudiantes/importar-estudiantes.component.html`

```html
<div class="container importar-estudiantes">
  <div class="header">
    <h1>Importar Estudiantes desde Excel</h1>
    <p class="descripcion">Carga masivamente estudiantes desde un archivo Excel y crea automáticamente sus usuarios y credenciales de acceso.</p>
  </div>

  <!-- Mostrar errores -->
  <div *ngIf="error" class="alert alert-danger">
    <strong>{{ error.error }}</strong>
    <p *ngIf="error.detalles">{{ error.detalles }}</p>
  </div>

  <!-- Formulario de importación (mostrar si no hay resultados) -->
  <div *ngIf="!mostrarResultados" class="formulario-section">
    <form [formGroup]="formularioImportacion" (ngSubmit)="iniciarImportacion()">
      
      <!-- Selección de Sede -->
      <div class="form-group">
        <label for="sede" class="form-label">Sede *</label>
        <select 
          id="sede" 
          formControlName="sede" 
          class="form-control"
          (change)="onSedeSeleccionada($event.target.value)"
          [disabled]="cargando">
          <option value="">-- Selecciona una sede --</option>
          <option *ngFor="let sede of sedesDisponibles" [value]="sede.idSede">
            {{ sede.nombre }}
          </option>
        </select>
        <small *ngIf="formularioImportacion.get('sede')?.hasError('required')" class="text-danger">
          La sede es requerida
        </small>
      </div>

      <!-- Selección de Archivo -->
      <div class="form-group">
        <label for="archivo" class="form-label">Archivo Excel (.xlsx) *</label>
        <div class="custom-file-input">
          <input 
            type="file" 
            id="archivo" 
            accept=".xlsx"
            (change)="onArchivoSeleccionado($event)"
            [disabled]="cargando"
            class="form-control">
          <p *ngIf="archivoSeleccionado" class="archivo-seleccionado">
            ✓ {{ archivoSeleccionado.name }}
          </p>
        </div>
        <small class="texto-ayuda">
          Selecciona un archivo Excel 2007+ (.xlsx) con la estructura de inscripción
        </small>
      </div>

      <!-- Barra de progreso (mostrar si está cargando) -->
      <div *ngIf="cargando" class="progreso-section">
        <p>Cargando: {{ progreso }}%</p>
        <div class="progress">
          <div class="progress-bar progress-bar-striped progress-bar-animated" 
               [style.width.%]="progreso">
          </div>
        </div>
      </div>

      <!-- Botones -->
      <div class="form-buttons">
        <button 
          type="submit" 
          class="btn btn-primary"
          [disabled]="!formularioImportacion.valid || cargando">
          <span *ngIf="!cargando">Iniciar Importación</span>
          <span *ngIf="cargando">Importando...</span>
        </button>
        <button 
          type="reset" 
          class="btn btn-secondary"
          (click)="reiniciar()"
          [disabled]="cargando">
          Limpiar
        </button>
      </div>

    </form>
  </div>

  <!-- Resultados de importación (mostrar si hay respuesta) -->
  <div *ngIf="mostrarResultados && respuestaImportacion" class="resultados-section">
    
    <!-- Resumen -->
    <div class="resumen-importacion">
      <h2>Resumen de Importación</h2>
      <div class="estadisticas">
        <div class="stat-card exitosos">
          <h3>{{ respuestaImportacion.exitosos }}</h3>
          <p>Exitosos</p>
        </div>
        <div class="stat-card errores">
          <h3>{{ respuestaImportacion.errores }}</h3>
          <p>Errores</p>
        </div>
        <div class="stat-card total">
          <h3>{{ respuestaImportacion.total }}</h3>
          <p>Total Procesado</p>
        </div>
        <div class="stat-card tasa-exito">
          <h3>{{ obtenerEstadisticas()?.tasaExito || 0 }}%</h3>
          <p>Tasa de Éxito</p>
        </div>
      </div>
    </div>

    <!-- Tabla de resultados -->
    <div class="tabla-resultados">
      <h3>Detalle de Importación</h3>
      <div class="tabla-scroll">
        <table class="tabla">
          <thead>
            <tr>
              <th>Fila</th>
              <th>Nombre</th>
              <th>Estado</th>
              <th>Email</th>
              <th>Contraseña</th>
              <th>Mensaje</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let resultado of respuestaImportacion.resultados" 
                [class]="'estado-' + obtenerClaseEstado(resultado)">
              <td>{{ resultado.fila }}</td>
              <td>{{ resultado.nombre }}</td>
              <td>
                <span class="badge" [class]="'badge-' + obtenerClaseEstado(resultado)">
                  {{ obtenerIconoEstado(resultado) }} {{ resultado.estado }}
                </span>
              </td>
              <td>{{ resultado.email || '-' }}</td>
              <td>{{ resultado.password || '-' }}</td>
              <td>{{ resultado.mensaje }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Botones de acción -->
    <div class="acciones-resultados">
      <button (click)="descargarReporte()" class="btn btn-info">
        📥 Descargar Reporte CSV
      </button>
      <button (click)="reiniciar()" class="btn btn-primary">
        ➕ Nueva Importación
      </button>
    </div>

  </div>

</div>
```

### 3. Crear archivo: `src/app/components/importar-estudiantes/importar-estudiantes.component.scss`

```scss
.importar-estudiantes {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;

  .header {
    margin-bottom: 30px;
    text-align: center;

    h1 {
      font-size: 2rem;
      color: #333;
      margin-bottom: 10px;
    }

    .descripcion {
      font-size: 1rem;
      color: #666;
    }
  }

  // Alertas
  .alert {
    padding: 15px;
    margin-bottom: 20px;
    border-radius: 5px;

    &.alert-danger {
      background-color: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;

      strong {
        display: block;
        margin-bottom: 10px;
      }
    }
  }

  // Formulario
  .formulario-section {
    background-color: #fff;
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    margin-bottom: 30px;

    .form-group {
      margin-bottom: 20px;

      .form-label {
        display: block;
        margin-bottom: 8px;
        font-weight: 600;
        color: #333;
      }

      .form-control {
        width: 100%;
        padding: 10px;
        border: 1px solid #ddd;
        border-radius: 5px;
        font-size: 1rem;
        transition: border-color 0.3s;

        &:focus {
          outline: none;
          border-color: #007bff;
          box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
        }

        &:disabled {
          background-color: #f5f5f5;
          cursor: not-allowed;
        }
      }

      .custom-file-input {
        position: relative;

        input[type="file"] {
          cursor: pointer;
        }

        .archivo-seleccionado {
          margin-top: 10px;
          color: #28a745;
          font-weight: 500;
        }
      }

      .texto-ayuda {
        display: block;
        margin-top: 8px;
        color: #666;
        font-size: 0.875rem;
      }

      .text-danger {
        color: #dc3545;
        font-size: 0.875rem;
      }
    }

    // Progreso
    .progreso-section {
      margin: 20px 0;

      p {
        margin-bottom: 10px;
        color: #666;
      }

      .progress {
        height: 25px;
        background-color: #e9ecef;
        border-radius: 5px;
        overflow: hidden;

        .progress-bar {
          background-color: #007bff;
          transition: width 0.3s;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-weight: 600;
          font-size: 0.875rem;
        }

        .progress-bar-striped {
          background-image: linear-gradient(
            45deg,
            rgba(255, 255, 255, 0.15) 25%,
            transparent 25%,
            transparent 50%,
            rgba(255, 255, 255, 0.15) 50%,
            rgba(255, 255, 255, 0.15) 75%,
            transparent 75%,
            transparent
          );
          background-size: 1rem 1rem;
        }

        .progress-bar-animated {
          animation: progress-bar-stripes 1s linear infinite;
        }

        @keyframes progress-bar-stripes {
          0% {
            background-position: 0 0;
          }
          100% {
            background-position: 1rem 0;
          }
        }
      }
    }

    // Botones
    .form-buttons {
      display: flex;
      gap: 10px;
      margin-top: 25px;

      .btn {
        padding: 10px 20px;
        border: none;
        border-radius: 5px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s;

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        &.btn-primary {
          background-color: #007bff;
          color: white;

          &:hover:not(:disabled) {
            background-color: #0056b3;
          }
        }

        &.btn-secondary {
          background-color: #6c757d;
          color: white;

          &:hover:not(:disabled) {
            background-color: #545b62;
          }
        }
      }
    }
  }

  // Resultados
  .resultados-section {
    animation: slideIn 0.3s ease-in;

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .resumen-importacion {
      background-color: #fff;
      padding: 30px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      margin-bottom: 30px;

      h2 {
        font-size: 1.5rem;
        margin-bottom: 20px;
        color: #333;
      }

      .estadisticas {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 15px;

        .stat-card {
          padding: 20px;
          border-radius: 8px;
          text-align: center;
          color: white;

          h3 {
            font-size: 2rem;
            margin: 0 0 10px 0;
          }

          p {
            margin: 0;
            font-size: 0.875rem;
            opacity: 0.9;
          }

          &.exitosos {
            background: linear-gradient(135deg, #28a745, #20c997);
          }

          &.errores {
            background: linear-gradient(135deg, #dc3545, #fd7e14);
          }

          &.total {
            background: linear-gradient(135deg, #007bff, #0dcaf0);
          }

          &.tasa-exito {
            background: linear-gradient(135deg, #6f42c1, #a8e6cf);
          }
        }
      }
    }

    .tabla-resultados {
      background-color: #fff;
      padding: 30px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      margin-bottom: 30px;

      h3 {
        font-size: 1.25rem;
        margin-bottom: 20px;
        color: #333;
      }

      .tabla-scroll {
        overflow-x: auto;

        .tabla {
          width: 100%;
          border-collapse: collapse;

          thead {
            background-color: #f8f9fa;

            th {
              padding: 12px;
              text-align: left;
              font-weight: 600;
              color: #333;
              border-bottom: 2px solid #dee2e6;
            }
          }

          tbody {
            tr {
              border-bottom: 1px solid #dee2e6;
              transition: background-color 0.3s;

              &:hover {
                background-color: #f8f9fa;
              }

              &.estado-exitoso {
                background-color: rgba(40, 167, 69, 0.05);
              }

              &.estado-error {
                background-color: rgba(220, 53, 69, 0.05);
              }

              td {
                padding: 12px;

                .badge {
                  display: inline-block;
                  padding: 5px 10px;
                  border-radius: 20px;
                  font-size: 0.875rem;
                  font-weight: 600;

                  &.badge-exitoso {
                    background-color: #28a745;
                    color: white;
                  }

                  &.badge-error {
                    background-color: #dc3545;
                    color: white;
                  }
                }
              }
            }
          }
        }
      }
    }

    .acciones-resultados {
      display: flex;
      gap: 10px;
      justify-content: flex-end;

      .btn {
        padding: 10px 20px;
        border: none;
        border-radius: 5px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s;

        &.btn-info {
          background-color: #17a2b8;
          color: white;

          &:hover {
            background-color: #138496;
          }
        }

        &.btn-primary {
          background-color: #007bff;
          color: white;

          &:hover {
            background-color: #0056b3;
          }
        }
      }
    }
  }
}

// Responsive
@media (max-width: 768px) {
  .importar-estudiantes {
    padding: 10px;

    .header h1 {
      font-size: 1.5rem;
    }

    .formulario-section,
    .resultados-section .resumen-importacion,
    .resultados-section .tabla-resultados {
      padding: 15px;
    }

    .resultados-section .estadisticas {
      grid-template-columns: repeat(2, 1fr);
    }

    .form-buttons,
    .acciones-resultados {
      flex-direction: column;
    }

    .btn {
      width: 100%;
    }
  }
}
```

---

## 🛣️ RUTAS

### Crear archivo: `src/app/app.routes.ts` (o actualizar)

```typescript
import { Routes } from '@angular/router';
import { ImportarEstudiantesComponent } from './components/importar-estudiantes/importar-estudiantes.component';

export const routes: Routes = [
  {
    path: 'estudiantes/importar',
    component: ImportarEstudiantesComponent,
    data: { title: 'Importar Estudiantes desde Excel' }
  },
  // Otras rutas...
];
```

### Alternativa para routing con módulos (si aplica):

```typescript
// app-routing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ImportarEstudiantesComponent } from './components/importar-estudiantes/importar-estudiantes.component';

const routes: Routes = [
  {
    path: 'estudiantes',
    children: [
      {
        path: 'importar',
        component: ImportarEstudiantesComponent
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
```

---

## ✅ VALIDACIONES

### Validaciones Necesarias en el Frontend

```typescript
/**
 * src/app/validators/excel-validators.ts
 */
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class ExcelValidators {

  /**
   * Valida que sea un archivo Excel
   */
  static archivoExcel(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const archivo = control.value as File;
      const nombreValido = archivo.name.toLowerCase().endsWith('.xlsx');
      const tipoValido = archivo.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

      return (nombreValido && tipoValido) ? null : { 'archivoInvalido': true };
    };
  }

  /**
   * Valida tamaño máximo de archivo
   */
  static tamanoMaximo(tamanoBytesMax: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const archivo = control.value as File;
      return archivo.size > tamanoBytesMax ? { 'tamanoExcedido': true } : null;
    };
  }

  /**
   * Valida que la sede esté seleccionada
   */
  static sedeSeleccionada(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const valor = control.value;
      const esValida = valor && !isNaN(parseInt(valor, 10)) && parseInt(valor, 10) > 0;
      return esValida ? null : { 'sedeNoValida': true };
    };
  }
}
```

### Usar validadores personalizados:

```typescript
// En el componente
private crearFormulario(): FormGroup {
  return this.fb.group({
    sede: ['', [
      Validators.required,
      ExcelValidators.sedeSeleccionada()
    ]],
    archivo: ['', [
      Validators.required,
      ExcelValidators.archivoExcel(),
      ExcelValidators.tamanoMaximo(5242880) // 5MB
    ]]
  });
}
```

---

## 🚨 MANEJO DE ERRORES

### Crear archivo: `src/app/interceptors/error.interceptor.ts`

```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor() { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error HTTP:', error);

        let mensajeError = 'Ocurrió un error desconocido';

        if (error.error instanceof ErrorEvent) {
          // Error del lado del cliente
          mensajeError = `Error: ${error.error.message}`;
        } else {
          // Error del servidor
          if (error.status === 0) {
            mensajeError = 'No se puede conectar al servidor';
          } else if (error.status === 400) {
            mensajeError = error.error?.detalles || 'Solicitud inválida';
          } else if (error.status === 404) {
            mensajeError = 'El recurso no fue encontrado';
          } else if (error.status === 500) {
            mensajeError = 'Error interno del servidor';
          } else if (error.status === 413) {
            mensajeError = 'El archivo es demasiado grande';
          }
        }

        return throwError(() => ({
          error: mensajeError,
          detalles: error.error?.detalles || error.message,
          status: error.status
        }));
      })
    );
  }
}
```

### Registrar el interceptor en `src/app/app.config.ts`:

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { ErrorInterceptor } from './interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([ErrorInterceptor])
    )
  ]
};
```

---

## 🧪 TESTING

### Crear archivo: `src/app/services/excel-import.service.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ExcelImportService } from './excel-import.service';
import { ExcelImportResponseDTO } from '../models/excel-import.model';

describe('ExcelImportService', () => {
  let service: ExcelImportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExcelImportService]
    });

    service = TestBed.inject(ExcelImportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debería crearse el servicio', () => {
    expect(service).toBeTruthy();
  });

  it('debería importar estudiantes correctamente', () => {
    const archivoMock = new File(['test'], 'test.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const respuestaMock: ExcelImportResponseDTO = {
      exitosos: 10,
      errores: 0,
      total: 10,
      resultados: []
    };

    service.importarEstudiantesDesdeExcel(archivoMock, 1).subscribe(respuesta => {
      expect(respuesta.exitosos).toBe(10);
      expect(respuesta.errores).toBe(0);
    });

    const req = httpMock.expectOne(request => 
      request.url.includes('/api/estudiantes/importar-excel')
    );

    expect(req.request.method).toBe('POST');
    req.flush(respuestaMock);
  });

  it('debería rechazar archivos no Excel', (done) => {
    const archivoMock = new File(['test'], 'test.txt', { type: 'text/plain' });

    service.importarEstudiantesDesdeExcel(archivoMock, 1).subscribe(
      () => {
        fail('Debería haber fallado');
      },
      error => {
        expect(error.error).toContain('xlsx');
        done();
      }
    );
  });
});
```

### Crear archivo: `src/app/components/importar-estudiantes/importar-estudiantes.component.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ImportarEstudiantesComponent } from './importar-estudiantes.component';
import { ExcelImportService } from '../../services/excel-import.service';
import { SedeService } from '../../services/sede.service';
import { of } from 'rxjs';

describe('ImportarEstudiantesComponent', () => {
  let component: ImportarEstudiantesComponent;
  let fixture: ComponentFixture<ImportarEstudiantesComponent>;
  let excelService: jasmine.SpyObj<ExcelImportService>;
  let sedeService: jasmine.SpyObj<SedeService>;

  beforeEach(async () => {
    const excelServiceSpy = jasmine.createSpyObj('ExcelImportService', [
      'importarEstudiantesDesdeExcel',
      'calcularEstadisticas',
      'descargarReporte'
    ]);

    const sedeServiceSpy = jasmine.createSpyObj('SedeService', ['obtenerSedes']);

    await TestBed.configureTestingModule({
      declarations: [ImportarEstudiantesComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: ExcelImportService, useValue: excelServiceSpy },
        { provide: SedeService, useValue: sedeServiceSpy }
      ]
    }).compileComponents();

    excelService = TestBed.inject(ExcelImportService) as jasmine.SpyObj<ExcelImportService>;
    sedeService = TestBed.inject(SedeService) as jasmine.SpyObj<SedeService>;

    fixture = TestBed.createComponent(ImportarEstudiantesComponent);
    component = fixture.componentInstance;
  });

  it('debería crearse el componente', () => {
    expect(component).toBeTruthy();
  });

  it('debería cargar las sedes al inicializar', () => {
    sedeService.obtenerSedes.and.returnValue(of([
      { idSede: 1, nombre: 'Sede 1' },
      { idSede: 2, nombre: 'Sede 2' }
    ]));

    component.ngOnInit();

    expect(component.sedesDisponibles.length).toBe(2);
  });
});
```

---

## 💡 BUENAS PRÁCTICAS

### 1. Seguridad

```typescript
// ✓ CORRECTO - Validar en el cliente Y servidor
if (!this.archivoSeleccionado.name.toLowerCase().endsWith('.xlsx')) {
  // Mostrar error
}

// ✗ INCORRECTO - Solo confiar en validación del cliente
// El servidor TAMBIÉN debe validar
```

### 2. Manejo de Memoria

```typescript
// ✓ CORRECTO - Usar takeUntil para evitar memory leaks
private destroy$ = new Subject<void>();

this.service.method$
  .pipe(takeUntil(this.destroy$))
  .subscribe(...);

ngOnDestroy(): void {
  this.destroy$.next();
  this.destroy$.complete();
}

// ✗ INCORRECTO - Suscripciones sin limpiar
this.service.method$.subscribe(...); // Memory leak
```

### 3. Manejo de Errores

```typescript
// ✓ CORRECTO - Capturar y procesar errores específicos
.pipe(
  catchError((error: HttpErrorResponse) => {
    if (error.status === 400) {
      // Manejar 400
    } else if (error.status === 500) {
      // Manejar 500
    }
    return throwError(() => error);
  })
)

// ✗ INCORRECTO - No diferenciar tipos de error
.pipe(
  catchError(error => throwError(() => error))
)
```

### 4. Reactividad

```typescript
// ✓ CORRECTO - Usar async pipe
<p>{{ progreso$ | async }}%</p>

// O RxJS operators
this.progreso$
  .pipe(
    map(p => p > 50 ? 'warning' : 'info')
  )
  .subscribe(clase => this.claseProgreso = clase);

// ✗ INCORRECTO - Muchas suscripciones manuales
this.service1.subscribe(...);
this.service2.subscribe(...);
this.service3.subscribe(...);
```

### 5. Validación

```typescript
// ✓ CORRECTO - Validar antes y después
if (!archivo || !archivo.name.endsWith('.xlsx')) {
  return throwError(() => 'Inválido');
}

// El servidor TAMBIÉN valida
// Nunca confiar solo en cliente

// ✗ INCORRECTO - No validar en el cliente
// Esperar errores del servidor
```

### 6. Interfaz de Usuario

```typescript
// ✓ CORRECTO - Deshabilitar botones mientras se procesa
<button [disabled]="cargando">Importar</button>

// Mostrar progreso
<div *ngIf="cargando">{{ progreso }}%</div>

// ✗ INCORRECTO - Permitir múltiples clics
<button (click)="iniciarImportacion()">Importar</button>
// Sin deshabilitar, puede generar múltiples peticiones
```

---

## 📦 ESTRUCTURA FINAL DEL PROYECTO

```
src/
├── app/
│   ├── components/
│   │   └── importar-estudiantes/
│   │       ├── importar-estudiantes.component.ts
│   │       ├── importar-estudiantes.component.html
│   │       ├── importar-estudiantes.component.scss
│   │       └── importar-estudiantes.component.spec.ts
│   ├── models/
│   │   └── excel-import.model.ts
│   ├── services/
│   │   ├── excel-import.service.ts
│   │   ├── excel-import.service.spec.ts
│   │   ├── sede.service.ts
│   │   └── sede.service.spec.ts
│   ├── validators/
│   │   └── excel-validators.ts
│   ├── interceptors/
│   │   └── error.interceptor.ts
│   ├── app.routes.ts (o app-routing.module.ts)
│   └── app.config.ts (o app.module.ts)
└── environments/
    ├── environment.ts
    └── environment.prod.ts
```

---

## 🌍 CONFIGURACIÓN DE ENTORNO

### Crear archivo: `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  apiEndpoints: {
    importarEstudiantes: '/api/estudiantes/importar-excel',
    obtenerSedes: '/api/sedes'
  }
};
```

### Usar en servicio:

```typescript
import { environment } from '../../../environments/environment';

export class ExcelImportService {
  private apiUrl = environment.apiUrl + environment.apiEndpoints.importarEstudiantes;
  // ...
}
```

---

## ✨ RESUMEN DE IMPLEMENTACIÓN FRONTEND

### Paso 1: Instalar Dependencias
```bash
npm install
```

### Paso 2: Crear Estructura
- Copiar archivos de modelos
- Crear servicios
- Crear componentes

### Paso 3: Registrar en App Config
- Agregar rutas
- Registrar interceptadores
- Configurar providers HTTP

### Paso 4: Usar en Template
- Importar en módulo/componente
- Usar etiqueta `<app-importar-estudiantes></app-importar-estudiantes>`

### Paso 5: Probar
- Verificar conexión al backend
- Probar con archivo Excel válido
- Verificar respuesta y visualización

---

## 🔗 REFERENCIAS

- [Angular 17 Documentation](https://angular.io/docs)
- [RxJS Operators](https://rxjs.dev/api)
- [Angular Forms](https://angular.io/guide/forms)
- [Angular HTTP Client](https://angular.io/guide/http)
- [Angular Testing](https://angular.io/guide/testing)

---

**Estado**: LISTO PARA IMPLEMENTACIÓN  
**Versión**: 1.0  
**Fecha**: 16 de Febrero de 2026
