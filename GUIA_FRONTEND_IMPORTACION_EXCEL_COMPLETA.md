# 🎯 Guía Completa: Importación de Estudiantes - Frontend (Angular)

## 📋 Tabla de Contenidos
1. [Servicio Angular](#servicio-angular)
2. [Componente Principal](#componente-principal)
3. [Template HTML](#template-html)
4. [Estilos CSS](#estilos-css)
5. [Modelos y Interfaces](#modelos-e-interfaces)
6. [Integración en Módulo](#integración-en-módulo)
7. [Ejemplo de Uso](#ejemplo-de-uso)

---

## 🔧 Servicio Angular

**Archivo:** `src/app/services/excel-import.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface ImportResult {
  exitosos: number;
  errores: number;
  total: number;
  timestamp: string;
  resultados: ResultadoImporte[];
}

export interface ResultadoImporte {
  fila: number;
  nombreEstudiante: string;
  numeroDocumento: string;
  estado: 'exitoso' | 'error';
  email?: string;
  password?: string;
  mensaje?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ExcelImportService {
  
  private readonly API_URL = 'http://localhost:8080/api/estudiantes'; // Ajustar según ambiente
  
  constructor(private http: HttpClient) { }
  
  /**
   * Descarga la plantilla de Excel desde el backend
   * 
   * @returns Observable<Blob> - Archivo Excel
   */
  descargarPlantilla(): Observable<Blob> {
    return this.http.get(
      `${this.API_URL}/descargar-plantilla`,
      { responseType: 'blob' }
    ).pipe(
      catchError(error => {
        console.error('Error descargando plantilla:', error);
        return throwError(() => new Error('No se pudo descargar la plantilla'));
      })
    );
  }
  
  /**
   * Importa estudiantes desde un archivo Excel
   * 
   * @param file - Archivo Excel (.xlsx)
   * @param sedeId - ID de la sede destino
   * @returns Observable<ImportResult> - Resultado de la importación
   */
  importarEstudiantes(file: File, sedeId: number): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    
    return this.http.post<ImportResult>(
      `${this.API_URL}/importar-excel?sedeId=${sedeId}`,
      formData
    ).pipe(
      catchError(error => {
        console.error('Error importando estudiantes:', error);
        const mensaje = error.error?.error || 'Error en la importación';
        return throwError(() => new Error(mensaje));
      })
    );
  }
  
  /**
   * Valida que el archivo sea Excel válido
   * 
   * @param file - Archivo a validar
   * @returns {valid: boolean, error?: string}
   */
  validarArchivo(file: File): { valid: boolean; error?: string } {
    // Verificar extensión
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      return {
        valid: false,
        error: 'Solo se aceptan archivos .xlsx (Excel 2007+)'
      };
    }
    
    // Verificar tamaño (máximo 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      return {
        valid: false,
        error: `Archivo muy grande. Máximo 10MB (actual: ${(file.size / 1024 / 1024).toFixed(2)}MB)`
      };
    }
    
    return { valid: true };
  }
}
```

---

## 💻 Componente Principal

**Archivo:** `src/app/components/importar-estudiantes/importar-estudiantes.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { ExcelImportService, ImportResult, ResultadoImporte } from '../../services/excel-import.service';

@Component({
  selector: 'app-importar-estudiantes',
  templateUrl: './importar-estudiantes.component.html',
  styleUrls: ['./importar-estudiantes.component.css']
})
export class ImportarEstudiantesComponent implements OnInit {
  
  // Estado de la UI
  sedeSeleccionada: number | null = null;
  sedes: any[] = []; // Cargar desde backend
  
  // Estado del archivo
  archivoSeleccionado: File | null = null;
  nombreArchivo: string = '';
  
  // Estados de proceso
  descargandoPlantilla = false;
  importando = false;
  importacionCompleta = false;
  
  // Resultados
  resultadoImportacion: ImportResult | null = null;
  errorMessage: string = '';
  
  // Control de vista
  vista: 'principal' | 'resultados' = 'principal';
  
  constructor(
    private excelImportService: ExcelImportService
  ) { }
  
  ngOnInit(): void {
    // Cargar sedes disponibles
    this.cargarSedes();
  }
  
  /**
   * Carga las sedes disponibles
   * (Implementar según tu backend)
   */
  cargarSedes(): void {
    // Ejemplo con datos mock - reemplazar con servicio real
    this.sedes = [
      { id: 1, nombre: 'Sede Centro' },
      { id: 2, nombre: 'Sede Nororiental' },
      { id: 3, nombre: 'Sede Sur' }
    ];
  }
  
  /**
   * Descarga la plantilla de Excel
   */
  descargarPlantilla(): void {
    if (!this.sedeSeleccionada) {
      this.errorMessage = 'Debe seleccionar una sede primero';
      return;
    }
    
    this.descargandoPlantilla = true;
    this.errorMessage = '';
    
    this.excelImportService.descargarPlantilla().subscribe({
      next: (blob: Blob) => {
        this.descargarBlob(blob, 'plantilla-estudiantes.xlsx');
        this.descargandoPlantilla = false;
      },
      error: (error) => {
        this.errorMessage = 'Error al descargar la plantilla: ' + error.message;
        this.descargandoPlantilla = false;
      }
    });
  }
  
  /**
   * Maneja la selección de archivo
   */
  onArchivoSeleccionado(event: any): void {
    const file: File = event.target.files?.[0];
    
    if (!file) {
      this.archivoSeleccionado = null;
      this.nombreArchivo = '';
      return;
    }
    
    // Validar archivo
    const validacion = this.excelImportService.validarArchivo(file);
    if (!validacion.valid) {
      this.errorMessage = validacion.error || 'Archivo inválido';
      this.archivoSeleccionado = null;
      this.nombreArchivo = '';
      return;
    }
    
    this.archivoSeleccionado = file;
    this.nombreArchivo = file.name;
    this.errorMessage = '';
  }
  
  /**
   * Inicia la importación de estudiantes
   */
  importarEstudiantes(): void {
    // Validaciones previas
    if (!this.sedeSeleccionada) {
      this.errorMessage = 'Debe seleccionar una sede';
      return;
    }
    
    if (!this.archivoSeleccionado) {
      this.errorMessage = 'Debe seleccionar un archivo Excel';
      return;
    }
    
    // Confirmación
    if (!confirm('¿Está seguro de que desea importar estos estudiantes?')) {
      return;
    }
    
    this.importando = true;
    this.errorMessage = '';
    
    this.excelImportService.importarEstudiantes(
      this.archivoSeleccionado,
      this.sedeSeleccionada
    ).subscribe({
      next: (resultado: ImportResult) => {
        this.resultadoImportacion = resultado;
        this.importacionCompleta = true;
        this.vista = 'resultados';
        this.importando = false;
      },
      error: (error) => {
        this.errorMessage = 'Error en la importación: ' + error.message;
        this.importando = false;
      }
    });
  }
  
  /**
   * Limpia los datos y vuelve a la vista principal
   */
  volver(): void {
    this.vista = 'principal';
    this.archivoSeleccionado = null;
    this.nombreArchivo = '';
    this.resultadoImportacion = null;
    this.importacionCompleta = false;
    this.errorMessage = '';
    
    // Resetear input de archivo
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }
  }
  
  /**
   * Descarga un blob como archivo
   */
  private descargarBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }
  
  /**
   * Obtiene clase CSS para el estado
   */
  getEstadoClass(estado: string): string {
    return estado === 'exitoso' ? 'exitoso' : 'error';
  }
  
  /**
   * Calcula estadísticas
   */
  get estadisticas() {
    if (!this.resultadoImportacion) return null;
    
    const resultado = this.resultadoImportacion;
    const porcentajeExito = (resultado.exitosos / resultado.total * 100).toFixed(2);
    
    return {
      total: resultado.total,
      exitosos: resultado.exitosos,
      errores: resultado.errores,
      porcentajeExito: porcentajeExito
    };
  }
}
```

---

## 🎨 Template HTML

**Archivo:** `src/app/components/importar-estudiantes/importar-estudiantes.component.html`

```html
<div class="contenedor-importacion">
  
  <!-- VISTA PRINCIPAL -->
  <div *ngIf="vista === 'principal'" class="vista-principal">
    <div class="encabezado">
      <h1>📊 Importar Estudiantes Masivamente</h1>
      <p>Descargue la plantilla, complétela con los datos y suba el archivo</p>
    </div>
    
    <!-- Sección de Errores -->
    <div *ngIf="errorMessage" class="alerta alerta-error">
      <strong>⚠️ Error:</strong> {{ errorMessage }}
    </div>
    
    <!-- Sección de Selección de Sede -->
    <div class="seccion">
      <h2>1. Seleccionar Sede</h2>
      <select [(ngModel)]="sedeSeleccionada" class="select-sede">
        <option [value]="null">-- Seleccione una sede --</option>
        <option *ngFor="let sede of sedes" [value]="sede.id">
          {{ sede.nombre }}
        </option>
      </select>
    </div>
    
    <!-- Sección de Descargar Plantilla -->
    <div class="seccion">
      <h2>2. Descargar Plantilla</h2>
      <p class="instruccion">
        Haga clic para descargar la plantilla Excel con todos los campos y ejemplos
      </p>
      <button 
        (click)="descargarPlantilla()" 
        [disabled]="!sedeSeleccionada || descargandoPlantilla"
        class="btn btn-descargar">
        <span *ngIf="!descargandoPlantilla">📥 Descargar Plantilla</span>
        <span *ngIf="descargandoPlantilla">⏳ Descargando...</span>
      </button>
    </div>
    
    <!-- Sección de Carga de Archivo -->
    <div class="seccion">
      <h2>3. Seleccionar Archivo Completado</h2>
      <div class="zona-carga">
        <input 
          type="file" 
          #fileInput
          accept=".xlsx"
          (change)="onArchivoSeleccionado($event)"
          class="input-archivo"
          id="fileInput">
        
        <label for="fileInput" class="label-archivo">
          <div class="icono">📁</div>
          <div class="texto">
            <div class="principal">Seleccione el archivo o arrastre aquí</div>
            <div class="secundario">Solo archivos .xlsx (máximo 10MB)</div>
          </div>
        </label>
        
        <div *ngIf="nombreArchivo" class="archivo-seleccionado">
          ✓ {{ nombreArchivo }}
        </div>
      </div>
    </div>
    
    <!-- Sección de Importación -->
    <div class="seccion">
      <h2>4. Importar Estudiantes</h2>
      <button 
        (click)="importarEstudiantes()"
        [disabled]="!sedeSeleccionada || !archivoSeleccionado || importando"
        class="btn btn-importar">
        <span *ngIf="!importando">🚀 Importar Estudiantes</span>
        <span *ngIf="importando">⏳ Importando (esto puede tardar)...</span>
      </button>
    </div>
    
    <!-- Ayuda -->
    <div class="seccion info">
      <h3>📋 Información Importante</h3>
      <ul>
        <li><strong>Campos requeridos (*):</strong> Nombre Completo, Tipo Documento, Numero Documento, Fecha Nacimiento, Correo Estudiante</li>
        <li><strong>Formato de fecha:</strong> DD/MM/YYYY (ej: 21/11/2001)</li>
        <li><strong>Campos booleanos:</strong> Si/No (ej: Certificado Medico Deportivo)</li>
        <li><strong>Máximo 10MB:</strong> El archivo no debe exceder 10 megabytes</li>
        <li><strong>Ejemplos incluidos:</strong> La plantilla incluye 3 ejemplos que puede editar o eliminar</li>
      </ul>
    </div>
  </div>
  
  <!-- VISTA DE RESULTADOS -->
  <div *ngIf="vista === 'resultados' && resultadoImportacion" class="vista-resultados">
    <div class="encabezado-resultados">
      <h1>✅ Importación Completada</h1>
    </div>
    
    <!-- Resumen Estadístico -->
    <div class="resumen-estadistico">
      <div class="estadistica" [class.exitoso]="estadisticas?.exitosos > 0">
        <div class="numero">{{ estadisticas?.exitosos }}</div>
        <div class="label">Exitosos</div>
      </div>
      <div class="estadistica" [class.error]="estadisticas?.errores > 0">
        <div class="numero">{{ estadisticas?.errores }}</div>
        <div class="label">Con Error</div>
      </div>
      <div class="estadistica info">
        <div class="numero">{{ estadisticas?.porcentajeExito }}%</div>
        <div class="label">Tasa Éxito</div>
      </div>
    </div>
    
    <!-- Barra de Progreso -->
    <div class="barra-progreso">
      <div 
        class="relleno exitoso" 
        [style.width.%]="(estadisticas?.exitosos / estadisticas?.total * 100) || 0">
      </div>
      <div 
        class="relleno error" 
        [style.width.%]="(estadisticas?.errores / estadisticas?.total * 100) || 0">
      </div>
    </div>
    
    <!-- Tabla de Resultados Detallados -->
    <div class="tabla-contenedor">
      <h2>Detalle de Importación</h2>
      <table class="tabla-resultados">
        <thead>
          <tr>
            <th>Fila</th>
            <th>Nombre</th>
            <th>Documento</th>
            <th>Estado</th>
            <th>Mensaje</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let resultado of resultadoImportacion?.resultados" [class.fila-error]="resultado.estado === 'error'">
            <td class="fila">{{ resultado.fila }}</td>
            <td class="nombre">{{ resultado.nombreEstudiante }}</td>
            <td class="documento">{{ resultado.numeroDocumento }}</td>
            <td>
              <span class="estado" [class]="'estado-' + resultado.estado">
                {{ resultado.estado === 'exitoso' ? '✓ Exitoso' : '✗ Error' }}
              </span>
            </td>
            <td class="mensaje">{{ resultado.mensaje || resultado.password || 'Importado correctamente' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- Información Adicional -->
    <div class="informacion">
      <p>
        <strong>Timestamp:</strong> {{ resultadoImportacion?.timestamp | date:'dd/MM/yyyy HH:mm:ss' }}
      </p>
      <p *ngIf="resultadoImportacion?.resultados[0]?.password">
        <strong>⚠️ Nota:</strong> Las contraseñas temporales se muestran arriba y han sido enviadas a los correos de los estudiantes.
      </p>
    </div>
    
    <!-- Botones de Acción -->
    <div class="botones-accion">
      <button (click)="volver()" class="btn btn-volver">
        ← Volver e Importar Más
      </button>
      <button class="btn btn-descargar-reporte">
        📄 Descargar Reporte
      </button>
    </div>
  </div>
  
</div>
```

---

## 🎨 Estilos CSS

**Archivo:** `src/app/components/importar-estudiantes/importar-estudiantes.component.css`

```css
.contenedor-importacion {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

/* ========== ENCABEZADOS ========== */
.encabezado {
  text-align: center;
  margin-bottom: 40px;
  border-bottom: 3px solid #2196F3;
  padding-bottom: 20px;
}

.encabezado h1 {
  font-size: 32px;
  margin: 0 0 10px 0;
  color: #1976D2;
}

.encabezado p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.encabezado-resultados {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  border-radius: 8px;
  color: white;
}

.encabezado-resultados h1 {
  margin: 0;
  font-size: 28px;
}

/* ========== SECCIONES ========== */
.seccion {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  border-left: 4px solid #2196F3;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.seccion h2 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 18px;
  color: #1976D2;
}

.seccion.info {
  background: #E3F2FD;
  border-left-color: #2196F3;
}

.seccion.info h3 {
  margin-top: 0;
  color: #1976D2;
}

.seccion.info ul {
  margin: 0;
  padding-left: 20px;
}

.seccion.info li {
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
}

/* ========== SELECTS Y INPUTS ========== */
.select-sede {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.select-sede:focus {
  outline: none;
  border-color: #2196F3;
}

.select-sede:hover {
  border-color: #999;
}

/* ========== INSTRUCCIONES ========== */
.instruccion {
  font-size: 14px;
  color: #666;
  margin: 0 0 15px 0;
  font-style: italic;
}

/* ========== BOTONES ========== */
.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-descargar {
  background: #4CAF50;
  color: white;
  width: 100%;
}

.btn-descargar:hover:not(:disabled) {
  background: #45a049;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.btn-importar {
  background: #2196F3;
  color: white;
  width: 100%;
  font-size: 16px;
  padding: 15px;
}

.btn-importar:hover:not(:disabled) {
  background: #0b7dda;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.btn-volver {
  background: #757575;
  color: white;
}

.btn-volver:hover {
  background: #616161;
}

.btn-descargar-reporte {
  background: #FF9800;
  color: white;
}

.btn-descargar-reporte:hover {
  background: #F57C00;
}

/* ========== ZONA DE CARGA ========== */
.zona-carga {
  position: relative;
  border: 2px dashed #2196F3;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  background: #F5F5F5;
  cursor: pointer;
  transition: all 0.3s;
}

.zona-carga:hover {
  background: #E3F2FD;
  border-color: #1976D2;
}

.input-archivo {
  display: none;
}

.label-archivo {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

.icono {
  font-size: 48px;
  margin-bottom: 10px;
}

.texto .principal {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 5px;
}

.texto .secundario {
  font-size: 12px;
  color: #999;
}

.archivo-seleccionado {
  margin-top: 15px;
  padding: 10px;
  background: #C8E6C9;
  color: #2E7D32;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

/* ========== ALERTAS ========== */
.alerta {
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 14px;
}

.alerta-error {
  background: #FFEBEE;
  border-left: 4px solid #F44336;
  color: #C62828;
}

/* ========== RESULTADOS ========== */
.resumen-estadistico {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.estadistica {
  background: white;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-top: 4px solid #999;
}

.estadistica.exitoso {
  border-top-color: #4CAF50;
}

.estadistica.error {
  border-top-color: #F44336;
}

.estadistica.info {
  border-top-color: #2196F3;
}

.estadistica .numero {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.estadistica .label {
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.barra-progreso {
  display: flex;
  height: 30px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.relleno {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 12px;
  transition: width 0.5s ease;
}

.relleno.exitoso {
  background: #4CAF50;
}

.relleno.error {
  background: #F44336;
}

/* ========== TABLA ========== */
.tabla-contenedor {
  margin-bottom: 30px;
}

.tabla-contenedor h2 {
  color: #1976D2;
  margin-bottom: 15px;
}

.tabla-resultados {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.tabla-resultados thead {
  background: #2196F3;
  color: white;
}

.tabla-resultados th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  border-bottom: 2px solid #1976D2;
}

.tabla-resultados td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.tabla-resultados tbody tr:hover {
  background: #F5F5F5;
}

.tabla-resultados tr.fila-error {
  background: #FFEBEE;
}

.tabla-resultados .fila {
  font-weight: bold;
  color: #2196F3;
}

.tabla-resultados .estado {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 600;
}

.tabla-resultados .estado-exitoso {
  background: #C8E6C9;
  color: #2E7D32;
}

.tabla-resultados .estado-error {
  background: #FFCDD2;
  color: #C62828;
}

/* ========== INFORMACIÓN Y BOTONES ========== */
.informacion {
  background: #E3F2FD;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 14px;
  border-left: 4px solid #2196F3;
}

.informacion p {
  margin: 8px 0;
}

.botones-accion {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.botones-accion .btn {
  flex: 1;
}

/* ========== RESPONSIVE ========== */
@media (max-width: 768px) {
  .contenedor-importacion {
    padding: 10px;
  }
  
  .encabezado h1 {
    font-size: 24px;
  }
  
  .resumen-estadistico {
    grid-template-columns: 1fr;
  }
  
  .tabla-resultados {
    font-size: 12px;
  }
  
  .tabla-resultados td,
  .tabla-resultados th {
    padding: 8px;
  }
  
  .botones-accion {
    flex-direction: column;
  }
  
  .botones-accion .btn {
    flex: auto;
  }
}
```

---

## 📦 Modelos e Interfaces

**Archivo:** `src/app/services/excel-import.service.ts` (ya incluido en el servicio)

O como archivo separado `src/app/models/excel.model.ts`:

```typescript
export interface ImportResult {
  exitosos: number;
  errores: number;
  total: number;
  timestamp: string;
  resultados: ResultadoImporte[];
}

export interface ResultadoImporte {
  fila: number;
  nombreEstudiante: string;
  numeroDocumento: string;
  estado: 'exitoso' | 'error';
  email?: string;
  password?: string;
  mensaje?: string;
}
```

---

## 🔌 Integración en Módulo

**Archivo:** `src/app/app.module.ts`

```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { ImportarEstudiantesComponent } from './components/importar-estudiantes/importar-estudiantes.component';
import { ExcelImportService } from './services/excel-import.service';

@NgModule({
  declarations: [
    AppComponent,
    ImportarEstudiantesComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [ExcelImportService],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

---

## 💡 Ejemplo de Uso Completo

**En tu componente padre:**

```typescript
// app.component.html
<app-importar-estudiantes></app-importar-estudiantes>

// app.component.ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Sistema de Gestión de Estudiantes';
}
```

---

## 🔄 Flujo de Proceso Completo

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND (Angular)                    BACKEND (Spring Boot)  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Usuario selecciona sede                                  │
│     └─> Validar sede seleccionada                            │
│                                                               │
│  2. Click "Descargar Plantilla"                              │
│     ├─> GET /descargar-plantilla ──────────────────────────>│
│     │                                ✓ Genera Excel con:     │
│     │                                  • 44 columnas         │
│     │                                  • 3 ejemplos          │
│     │                                  • Headers formateados │
│     <────── Blob (archivo.xlsx) ──────────────────────────┤
│     ├─> Guardar archivo automáticamente                      │
│                                                               │
│  3. Usuario completa plantilla                               │
│     └─> Agrega/edita estudiantes                             │
│                                                               │
│  4. Click "Importar Estudiantes"                             │
│     ├─> Validar:                                             │
│     │   • Sede seleccionada                                  │
│     │   • Archivo seleccionado                               │
│     │   • Formato .xlsx                                      │
│     │   • Tamaño < 10MB                                      │
│     │   • Confirmación del usuario                           │
│     │                                                         │
│     ├─> POST /importar-excel                                 │
│     │   (FormData: file + sedeId) ────────────────────────>│
│     │                                ✓ Procesa Excel:        │
│     │                                  • Lee headers         │
│     │                                  • Mapea dinámicamente │
│     │                                  • Valida cada fila    │
│     │                                  • Crea Estudiantes    │
│     │                                  • Crea Usuarios       │
│     │                                  • Genera contraseñas  │
│     <────── ImportResult ────────────────────────────────┤
│     │   {                                                    │
│     │     exitosos: 3,                                       │
│     │     errores: 0,                                        │
│     │     total: 3,                                          │
│     │     resultados: [...]                                  │
│     │   }                                                    │
│     │                                                         │
│     ├─> Mostrar resultados                                   │
│     ├─> Tabla con detalles de importación                    │
│     └─> Opción para descargar reporte                        │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist para Frontend

- [ ] Crear `ExcelImportService` con métodos de descarga e importación
- [ ] Crear componente `ImportarEstudiantesComponent`
- [ ] Crear template HTML con formulario de importación
- [ ] Agregar estilos CSS para mejor UX
- [ ] Integrar en módulo principal
- [ ] Configurar URL correcta del backend
- [ ] Probar descarga de plantilla
- [ ] Probar importación de archivo
- [ ] Verificar manejo de errores
- [ ] Validar respuestas del backend
- [ ] Implementar feedback visual (spinners, mensajes)
- [ ] Testing con diferentes escenarios

---

## 🚨 Puntos Importantes

1. **URL del Backend:** Cambiar `http://localhost:8080` según ambiente
2. **Headers HTTP:** El servicio usa `Content-Type: multipart/form-data` automáticamente
3. **CORS:** Asegurar que el backend permite peticiones desde frontend
4. **Manejo de Errores:** El servicio retorna mensajes claros
5. **Validaciones:** Se hacen tanto en frontend (UX) como en backend (seguridad)
6. **Formato de Fechas:** El backend espera DD/MM/YYYY

---

## 📞 Preguntas Frecuentes

**P: ¿Qué pasa si hay errores de importación?**
R: El backend retorna un array con los errores específicos, el frontend muestra tabla detallada

**P: ¿Puedo importar estudiantes duplicados?**
R: No, el backend valida que documento + tipo de documento sea único

**P: ¿Dónde se guardan las contraseñas?**
R: Se generan, se guardan en BD, se devuelven en respuesta y se envían por email

**P: ¿Puedo importar más de 10MB?**
R: No, ambos frontend y backend validan máximo 10MB

**P: ¿Qué campos son requeridos?**
R: Nombre, Tipo Documento, Número Documento, Fecha Nacimiento, Correo Estudiante

