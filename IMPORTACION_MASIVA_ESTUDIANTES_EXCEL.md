# Implementación de Importación Masiva de Estudiantes desde Excel

## Descripción General

Se ha implementado un sistema completo para importar estudiantes masivamente desde archivos Excel (.xlsx). El sistema:

1. **Lee archivos Excel** con la estructura de inscripción de estudiantes
2. **Crea automáticamente usuarios** con credenciales de acceso
3. **Genera membresías** iniciales en estado PENDIENTE
4. **Valida datos** antes de guardar en la base de datos
5. **Retorna un reporte** detallado de éxitos y errores

## Archivos Creados/Modificados

### 1. **Dependencias Agregadas (pom.xml)**
```xml
<!-- Apache POI para leer archivos Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### 2. **Nuevos Archivos Creados**

#### **ExcelEstudianteImportDTO.java**
- DTO que mapea todas las columnas del Excel con los campos de la entidad Estudiante
- Contiene campos para información personal, contacto, médica, académica, deportiva, etc.
- Incluye validación de número de fila y estado de procesamiento

#### **ExcelImportService.java**
- Servicio responsable de leer y parsear archivos Excel
- Mapea datos de celdas a DTOs
- Maneja conversiones de tipos (strings, fechas, booleanos, números)
- Detecta y salta filas vacías

#### **ExcelImportResponseDTO.java**
- DTO para la respuesta de la importación
- Incluye conteos de exitosos/errores/total
- Lista detallada de resultados por estudiante

### 3. **Archivos Modificados**

#### **EstudianteService.java**
Se agregaron los siguientes métodos:

```java
/**
 * Importa masivamente estudiantes desde un archivo Excel
 * Crea automáticamente usuarios con credenciales (email y password)
 */
@Transactional
public Map<String, Object> importarEstudiantesDesdeExcel(
    List<ExcelEstudianteImportDTO> dtos, 
    Integer sedeId)

/**
 * Valida un DTO de estudiante del Excel
 */
private String validarDtoEstudiante(ExcelEstudianteImportDTO dto)

/**
 * Convierte un ExcelEstudianteImportDTO a una entidad Estudiante
 */
private Estudiante dtoAEstudiante(
    ExcelEstudianteImportDTO dto, 
    Sede sede)

/**
 * Importa estudiantes desde un archivo Excel
 */
@Transactional
public Map<String, Object> procesarImportacionExcel(
    java.io.InputStream inputStream, 
    Integer sedeId)
```

#### **EstudianteController.java**
Se agregó el endpoint:

```java
/**
 * Importar estudiantes masivamente desde un archivo Excel
 * POST /api/estudiantes/importar-excel?sedeId=1
 */
@PostMapping("/importar-excel")
public ResponseEntity<?> importarExcel(
    @RequestParam("file") MultipartFile file,
    @RequestParam("sedeId") Integer sedeId)
```

## Estructura del Excel Esperado

El archivo Excel debe contener las siguientes columnas en orden (a partir de la fila 2):

### Información Personal del Estudiante
- Nombres y Apellidos
- Tipo de Documento
- Número de Documento
- Fecha de Nacimiento
- Edad
- Sexo

### Información de Contacto
- Dirección de Residencia
- Barrio
- Celular de Contacto Estudiante
- Número de WhatsApp Estudiante
- Correo Electrónico (Estudiante)

### Información de Sede
- Sede (nombre de la sede)

### Información del Tutor/Padre
- Nombre del Padre/Madre/Tutor
- Parentesco
- Número de Documento del Tutor
- Teléfono del Tutor
- Correo del Tutor
- Ocupación/Profesión del Tutor

### Información Académica
- Institución Educativa del Estudiante
- Jornada
- Curso / Grado Actual (Solo número)

### Información Médica
- EPS / Entidad de Salud
- Tipo de Sangre
- Alergias
- Enfermedades o Condiciones Médicas
- Medicamentos que consume regularmente
- Cuenta con Certificado Médico Deportivo

### Información de Pagos
- Fechas de Pago en el Mes

### Contacto de Emergencia
- Nombre Contacto de Emergencia
- Teléfono del contacto de emergencia
- Parentesco del contacto de emergencia
- Ocupación del contacto de emergencia
- Correo electrónico del contacto de emergencia

### Poblaciones Vulnerables
- Personas LGBTIQ+
- Personas con Discapacidad
- Condición / Patología
- Migrantes, Refugiados y Solicitantes de Asilo
- Poblaciones Étnicas
- Religión

### Información Deportiva
- Experiencia en Voleibol
- Otras Disciplinas Practicadas
- Posición Preferida
- Dominancia
- Nivel Actual
- Clubes o Escuelas Anteriores

### Consentimiento Informado
- Declaro que he leído y acepto el consentimiento informado
- Firma digital (escriba su nombre completo)
- Fecha de diligenciamiento

## Cómo Usar

### 1. Preparar el Archivo Excel

- Crear un archivo .xlsx con la estructura descrita arriba
- La primera fila debe ser de encabezados (títulos)
- Los datos comienzan desde la fila 2
- Completar todos los campos requeridos

### 2. Llamar al Endpoint

```bash
POST /api/estudiantes/importar-excel?sedeId=1
Content-Type: multipart/form-data

file: [archivo.xlsx]
sedeId: 1
```

**Parámetros:**
- `file`: Archivo Excel multipart
- `sedeId`: ID de la sede a asignar a los estudiantes (requerido)

### 3. Respuesta Exitosa

```json
{
  "exitosos": 25,
  "errores": 2,
  "total": 27,
  "resultados": [
    {
      "fila": 2,
      "nombre": "Juan Pérez García",
      "estado": "EXITOSO",
      "mensaje": "Estudiante y usuario creados",
      "idEstudiante": 123,
      "email": "juan@example.com",
      "password": "1234567890"
    },
    {
      "fila": 3,
      "nombre": "María López",
      "estado": "ERROR",
      "mensaje": "El correo ya está registrado en el sistema"
    }
  ]
}
```

### 4. Respuesta de Error

```json
{
  "error": "El archivo debe ser de tipo .xlsx (Excel 2007+)"
}
```

## Lógica de Procesamiento

### Para Cada Estudiante:

1. **Validación**
   - Se valida que tenga nombre, documento, correo, fecha de nacimiento y tipo de documento
   - Se verifica que el email no esté registrado como usuario
   - Se verifica que el documento no esté registrado como estudiante

2. **Creación del Estudiante**
   - Se crea el registro en la tabla `estudiante`
   - Se asigna el estado de pago como `PENDIENTE`

3. **Creación de Membresía**
   - Se crea un registro en `membresia`
   - Estado inicial: `false` (pendiente/inactivo)
   - Fecha de inicio: HOY
   - Fecha de fin: HOY + 1 mes
   - Valor mensual: 50.000 (por defecto)

4. **Creación de Usuario**
   - Se crea un usuario con:
     - **Email**: El correo del estudiante
     - **Password**: El número de documento del estudiante
     - **Rol**: STUDENT (automático)
   - El usuario queda activo y habilitado para login

### Mapeo de Enumeraciones

El sistema convierte automáticamente valores de texto a enumeraciones:

- **Tipo de Documento**: TI, CC, RC, PASAPORTE
- **Sexo**: MASCULINO, FEMENINO, OTRO
- **Jornada**: MAÑANA, TARDE, NOCHE, UNICA
- **Dominancia**: DERECHA, IZQUIERDA, AMBIDIESTRO
- **Nivel Actual**: INICIANTE, INTERMEDIO, AVANZADO

## Validaciones

### Campos Requeridos
- Nombre Completo
- Número de Documento
- Correo Electrónico
- Fecha de Nacimiento
- Tipo de Documento

### Validaciones Previas
- No permite correos duplicados
- No permite documentos duplicados
- Valida que la sede exista

## Manejo de Errores

El sistema captura y reporta:

1. **Errores de Validación**
   - Campos faltantes
   - Datos duplicados
   - Valores inválidos para enumeraciones

2. **Errores de Procesamiento**
   - Excepción durante la creación del usuario
   - Excepción durante la creación de membresía
   - Problemas generales de base de datos

3. **Errores de Archivo**
   - Archivo vacío
   - Formato incorrecto (debe ser .xlsx)
   - Problemas en lectura del Excel

## Transaccionalidad

Cada importación es una transacción atómica:
- Si un estudiante falla, NO afecta a los otros
- Los cambios se guardan automáticamente
- En caso de error, se registra y continúa con el siguiente

## Credenciales Generadas

Para cada estudiante se crea automáticamente:

```
Email: [correo_del_estudiante]
Password: [numero_de_documento]
```

**Nota**: El estudiante debe cambiar su contraseña en el primer login

## Requisitos de Base de Datos

Las siguientes tablas deben existir y tener relaciones configuradas:
- `estudiante`
- `usuario`
- `membresia`
- `sede`
- `rol`

## Ejemplo de Uso desde Frontend (Angular/TypeScript)

```typescript
importarEstudiantes(file: File, sedeId: number): Observable<any> {
  const formData = new FormData();
  formData.append('file', file);
  
  return this.http.post(
    `${this.apiUrl}/estudiantes/importar-excel?sedeId=${sedeId}`,
    formData
  );
}

// Uso
this.estudianteService.importarEstudiantes(excelFile, 1).subscribe(
  (resultado) => {
    console.log(`Exitosos: ${resultado.exitosos}, Errores: ${resultado.errores}`);
    console.log(resultado.resultados);
  },
  (error) => {
    console.error('Error en importación', error);
  }
);
```

## Ejemplo de Uso desde cURL

```bash
curl -X POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1 \
  -F "file=@estudiantes.xlsx"
```

## Notas Importantes

1. **Contraseñas Iniciales**: Las contraseñas son el número de documento. Se recomienda forzar cambio en primer login.

2. **Emails Únicos**: El sistema requiere que cada estudiante tenga un email único.

3. **Formato de Fechas**: Las fechas se detectan automáticamente del formato Excel.

4. **Booleanos**: Los valores booleanos aceptan: "sí", "si", "true", "1" (case-insensitive).

5. **Rollback**: Si la transacción falla, todos los cambios se revierten.

6. **Performance**: Para archivos grandes (>1000 registros), considerar procesar en lotes.

## Troubleshooting

### Error: "El archivo debe ser de tipo .xlsx"
- Verificar que el archivo tenga extensión .xlsx
- Guardar el archivo en formato Excel 2007 o superior

### Error: "El correo ya está registrado"
- Verificar que el estudiante no exista en el sistema
- Usar un correo diferente si es un registro nuevo

### Error: "La sede especificada no existe"
- Verificar el ID de la sede
- Crear la sede antes de importar estudiantes

### Error: Campos vacíos no se procesan
- Usar valores por defecto cuando sea posible
- Solo campos requeridos son obligatorios

## Próximas Mejoras Sugeridas

1. Agregar opción para actualizar estudiantes existentes
2. Permitir customizar campos requeridos
3. Agregar notificaciones por email con credenciales
4. Exportar reporte de importación en Excel
5. Agregar histórico de importaciones
6. Permitir importar por lotes desde UI

---

**Versión**: 1.0  
**Fecha de Creación**: 16/02/2026  
**Estado**: Producción
