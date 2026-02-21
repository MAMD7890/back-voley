# 📋 GUÍA COMPLETA: IMPORTACIÓN DE EXCEL DE ESTUDIANTES - PARA FRONTEND

## 🔗 FLUJO COMPLETO DEL PROCESO

```
FRONTEND (Angular)
    ↓
[Usuario selecciona archivo Excel]
    ↓
POST /api/estudiantes/importar-excel?sedeId=2 (multipart/form-data)
    ↓
BACKEND - EstudianteController.importarExcel()
    ↓
EstudianteService.procesarImportacionExcelConUsuarios()
    ↓
ExcelImportService.leerExcel()  ← Parsea el Excel
    ↓
Validar cada fila de datos
    ↓
Crear Estudiante + Usuario + Rol STUDENT
    ↓
Devuelve ExcelImportResponseDTO
    ↓
FRONTEND: Mostrar resultados
```

---

## � ¡TU EXCEL FALLÓ? AQUÍ ESTÁ EL PROBLEMA

**Si viste esta respuesta:**
```
Exitosos: 0
Errores: 10
Total: 10
```

**Significa:** Tu archivo Excel tiene la estructura **INCORRECTA**.

👉 **OPCIÓN RÁPIDA (5 min):** [GUIA_SOLUCION_RAPIDA.md](GUIA_SOLUCION_RAPIDA.md) ← EMPIEZA AQUÍ

👉 **Quiero copiar/pegar:** [PLANTILLA_EXCEL_COPIAR_PEGAR.md](PLANTILLA_EXCEL_COPIAR_PEGAR.md)

👉 **Quiero entender el problema:** [VISUALIZACION_PROBLEMA_SOLUCION.md](VISUALIZACION_PROBLEMA_SOLUCION.md)

👉 **Necesito depuración detallada:** [DIAGNOSTICO_EXCEL_FALLIDO.md](DIAGNOSTICO_EXCEL_FALLIDO.md)

👉 **Paso a paso completo:** [RECUPERACION_RAPIDA_5_MINUTOS.md](RECUPERACION_RAPIDA_5_MINUTOS.md)

---

## �📊 ESTRUCTURA CORRECTA DEL ARCHIVO EXCEL

### Requisitos:
- ✅ Formato: `.xlsx` (Excel 2007+)
- ✅ Tamaño máximo: 10 MB
- ✅ Encabezados en FILA 1
- ✅ Datos comienzan en FILA 2
- ✅ NO dejar filas vacías en medio

### Columnas Obligatorias (EN ESTE ORDEN):

| # | Nombre Columna | Tipo | Formato | Ejemplo | Validación |
|---|---|---|---|---|---|
| A | **nombreCompleto** | Texto | No números | `Juan Pérez García` | Requerido, mín 3 caracteres |
| B | **tipoDocumento** | Texto | Opciones válidas | `Cédula` | Requerido |
| C | **numeroDocumento** | Número/Texto | Sin caracteres especiales | `1234567890` | Requerido, único |
| D | **fechaNacimiento** | Fecha | `DD/MM/YYYY` | `21/11/2001` | **Requerido, formato exacto** |
| E | **correoEstudiante** | Email | RFC válido | `juan@example.com` | Requerido, único |

### Ejemplo Excel Correcto:

```
Fila 1 (ENCABEZADOS):
A1: nombreCompleto | B1: tipoDocumento | C1: numeroDocumento | D1: fechaNacimiento | E1: correoEstudiante

Fila 2 (DATOS):
A2: Juan Pérez García | B2: Cédula | C2: 1234567890 | D2: 21/11/2001 | E2: juan.perez@example.com

Fila 3 (DATOS):
A3: María López Rodríguez | B3: Cédula | C3: 9876543210 | D3: 15/03/2002 | E3: maria.lopez@example.com

Fila 4 (DATOS):
A4: Carlos Gómez Martínez | B4: Cédula | C4: 5555555555 | D4: 10/07/2001 | E4: carlos.gomez@example.com
```

---

## 🚨 ERRORES COMUNES Y SOLUCIONES

### ❌ Error: "Fecha de nacimiento requerida"
**Causa**: Celda de fecha vacía o en formato incorrecto
**Solución**: 
- Asegúrate que columna D NO esté vacía
- Formato EXACTO: `DD/MM/YYYY` (ej: `21/11/2001`)
- NO usar: `2001-11-21`, `21-11-2001`, `21.11.2001`

### ❌ Error: "Correo electrónico requerido"
**Causa**: Celda de email vacía
**Solución**: 
- Llena TODAS las celdas de la columna E
- Formato válido: `usuario@dominio.com`

### ❌ Error: "Nombre completo requerido"
**Causa**: Celda de nombre vacía
**Solución**: 
- Llena TODAS las celdas de la columna A
- Mínimo 3 caracteres

### ❌ Error: "Número de documento requerido"
**Causa**: Celda de documento vacía
**Solución**: 
- Llena TODAS las celdas de la columna C
- Sin caracteres especiales

---

## 🛠️ SERVICIOS Y CONTROLADORES BACKEND

### 1. **EstudianteController.java** (Punto de entrada)
```
Endpoint: POST /api/estudiantes/importar-excel?sedeId={id}
Parámetros:
  - sedeId (query): ID de la sede donde se registran los estudiantes
  - file (form-data): Archivo .xlsx multipart/form-data

Respuesta exitosa: HTTP 200
{
  "exitosos": 3,
  "errores": 0,
  "total": 3,
  "mensaje": "Importación completada: 3 exitosos, 0 errores",
  "detalles": [...]
}

Respuesta con errores: HTTP 200
{
  "exitosos": 0,
  "errores": 3,
  "total": 3,
  "mensaje": "Importación completada: 0 exitosos, 3 errores",
  "detalles": [
    {
      "fila": 2,
      "errores": ["Fecha de nacimiento requerida", "Email requerido"]
    }
  ]
}
```

### 2. **EstudianteService.procesarImportacionExcelConUsuarios()** (Lógica principal)
```
Flujo:
1. Valida que la sede existe (ID debe ser válido)
2. Valida que el rol STUDENT existe en BD
3. Llama a ExcelImportService.leerExcel(inputStream)
4. Para cada fila:
   - Valida campos obligatorios
   - Verifica que email NO exista ya en BD
   - Verifica que documento NO exista ya en BD
   - Crea Estudiante
   - Crea Usuario automático con rol STUDENT
   - Genera contraseña aleatoria
5. Retorna resumen con exitosos/errores
```

### 3. **ExcelImportService.leerExcel()** (Parseo de Excel)
```
Responsabilidades:
1. Lee archivo .xlsx usando Apache POI
2. Mapea cada fila a ExcelEstudianteImportDTO
3. PARSEA FECHAS en múltiples formatos:
   - DD/MM/YYYY ✅ (formato principal - Excel español)
   - D/M/YYYY ✅ (sin ceros - Excel flexible)
   - YYYY-MM-DD ✅ (ISO format - backup)
4. Retorna List<ExcelEstudianteImportDTO>

Formatos de fecha aceptados:
- 21/11/2001 ✅
- 21/3/2002 ✅
- 2001-11-21 ✅
- 2001-3-2 ✅
```

---

## 📝 CAMPOS ADICIONALES OPCIONALES (Para futuro)

El Excel también puede incluir (aunque no son validados actualmente):

| Columna | Campo | Tipo | Ejemplo |
|---------|-------|------|---------|
| F | celularEstudiante | Texto | 3001234567 |
| G | whatsappEstudiante | Texto | 3001234567 |
| H | direccionResidencia | Texto | Calle 10 #20-30 |
| I | sexo | Texto | Masculino/Femenino |
| J | institucionEducativa | Texto | Colegio XYZ |
| K | eps | Texto | EPS Salud |

---

## ✅ VALIDACIONES APLICADAS

Cada estudiante importado se valida contra:

1. ✅ **Campos obligatorios NO vacíos**
   - nombreCompleto
   - tipoDocumento
   - numeroDocumento
   - fechaNacimiento (formato DD/MM/YYYY)
   - correoEstudiante

2. ✅ **Formato de email válido** (RFC 5322)

3. ✅ **Unicidad en BD**
   - Email NO debe existir en tabla usuario
   - Número documento NO debe existir en tabla usuario

4. ✅ **Sede debe existir**
   - El sedeId del query param debe ser válido

5. ✅ **Rol STUDENT debe existir**
   - Se crea automáticamente al iniciar aplicación (schema.sql)

---

## 🔄 QUÉ OCURRE AL IMPORTAR

Para CADA fila exitosa:

### Tabla `estudiante`
```sql
INSERT INTO estudiante (
  nombre_completo,
  numero_documento,
  tipo_documento,
  fecha_nacimiento,
  correo_estudiante,
  celular_estudiante,
  estado,
  estado_pago,
  id_sede
) VALUES (
  'Juan Pérez García',
  '1234567890',
  'Cédula',
  '2001-11-21',      ← Se convierte a formato YYYY-MM-DD en BD
  'juan@example.com',
  NULL,
  true,
  'PENDIENTE',
  2
);
```

### Tabla `usuario` (creado automáticamente)
```sql
INSERT INTO usuario (
  nombre,
  email,
  numero_documento,
  tipo_documento,
  username,
  password,           ← Contraseña aleatoria hasheada
  requiere_changio_password,
  id_rol,
  estado,
  id_estudiante
) VALUES (
  'Juan Pérez García',
  'juan@example.com',
  '1234567890',
  'Cédula',
  'juan.perez',       ← Username generado automáticamente
  'hash_aleatorio',
  true,               ← Usuario debe cambiar password al primer login
  4,                  ← ID del rol STUDENT
  true,
  <id_del_estudiante_creado>
);
```

---

## 📥 INSTRUCCIONES PARA EL FRONTEND (Angular)

### 1. Crear componente de importación
```typescript
import { Component } from '@angular/core';
import { EstudianteService } from './services/estudiante.service';

@Component({
  selector: 'app-importar-excel',
  templateUrl: './importar-excel.component.html',
  styleUrls: ['./importar-excel.component.css']
})
export class ImportarExcelComponent {
  sedeId: number = 2;
  archivoSeleccionado: File | null = null;
  cargando: boolean = false;
  resultado: any = null;

  constructor(private estudianteService: EstudianteService) {}

  onArchivoSeleccionado(event: any) {
    this.archivoSeleccionado = event.target.files[0];
  }

  importar() {
    if (!this.archivoSeleccionado) {
      alert('Por favor selecciona un archivo');
      return;
    }

    this.cargando = true;
    this.estudianteService.importarExcel(this.archivoSeleccionado, this.sedeId)
      .subscribe(
        (response) => {
          this.resultado = response;
          this.cargando = false;
          alert(`Importación: ${response.exitosos} exitosos, ${response.errores} errores`);
        },
        (error) => {
          this.cargando = false;
          console.error('Error en importación:', error);
          alert('Error al importar: ' + error.error?.mensaje);
        }
      );
  }
}
```

### 2. Crear servicio
```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EstudianteService {
  private apiUrl = '/api/estudiantes';

  constructor(private http: HttpClient) {}

  importarExcel(archivo: File, sedeId: number): Observable<any> {
    const formData = new FormData();
    formData.append('file', archivo);

    return this.http.post(
      `${this.apiUrl}/importar-excel?sedeId=${sedeId}`,
      formData
    );
  }
}
```

### 3. Template HTML
```html
<div class="importar-excel">
  <h2>Importar Estudiantes desde Excel</h2>
  
  <div class="form-group">
    <label>Selecciona archivo Excel (.xlsx):</label>
    <input type="file" (change)="onArchivoSeleccionado($event)" accept=".xlsx"/>
  </div>

  <button 
    (click)="importar()" 
    [disabled]="cargando || !archivoSeleccionado"
    class="btn btn-primary">
    {{ cargando ? 'Importando...' : 'Importar' }}
  </button>

  <div *ngIf="resultado" class="resultado">
    <h3>Resultado de Importación</h3>
    <p>Exitosos: {{ resultado.exitosos }}</p>
    <p>Errores: {{ resultado.errores }}</p>
    <p>Total: {{ resultado.total }}</p>
    
    <div *ngIf="resultado.detalles && resultado.detalles.length > 0">
      <h4>Detalles de Errores:</h4>
      <ul>
        <li *ngFor="let detalle of resultado.detalles">
          Fila {{ detalle.fila }}: {{ detalle.errores.join(', ') }}
        </li>
      </ul>
    </div>
  </div>
</div>
```

---

## 🧪 PRUEBA CON CURL

```bash
curl -X POST \
  "http://localhost:8080/api/estudiantes/importar-excel?sedeId=2" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/ruta/al/archivo/plantilla-estudiantes.xlsx"
```

---

## 📌 RESUMEN PARA EL FRONTEND

1. **Crear archivo Excel** con estructura exacta (ver tabla arriba)
2. **Enviar POST** a `/api/estudiantes/importar-excel?sedeId=2`
3. **Incluir archivo** como `multipart/form-data` con key `file`
4. **Mostrar resultado** con cantidad de exitosos/errores
5. **En errores**: Mostrar qué fila falló y por qué

---

## 🔑 VARIABLES DE ENTORNO NECESARIAS (Backend)

Estas ya están configuradas, pero por si acaso:

```properties
# pom.xml tiene:
- org.apache.poi:poi:5.0.0
- org.apache.poi:poi-ooxml:5.0.0

# application.properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## ✨ LO QUE GENERA AUTOMÁTICAMENTE

Cuando importas 1 estudiante, el sistema AUTOMÁTICAMENTE:

✅ Crea registro en tabla `estudiante`  
✅ Crea usuario en tabla `usuario`  
✅ Asigna rol STUDENT automáticamente  
✅ Genera contraseña aleatoria hasheada  
✅ Crea username a partir del nombre  
✅ Marca como "requiere cambio de password"  
✅ Registra auditoría de importación  

**El estudiante puede hacer login inmediatamente después.**

---

## 📞 SOPORTE

Si encuentras errores:

1. **Verifica el Excel**: Usa exactamente la estructura de la tabla
2. **Revisa los logs** del backend en estación `/api/estudiantes/importar-excel`
3. **Confirma la fecha**: Debe ser DD/MM/YYYY (ej: 21/11/2001)
4. **Evita duplicados**: Email y documento deben ser únicos
5. **Valida sede**: El sedeId debe existir en BD

