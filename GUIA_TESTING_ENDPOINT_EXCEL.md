# ✅ VERIFICACIÓN DEL ENDPOINT - POST /api/estudiantes/importar-excel

## Estado: LISTO PARA PRUEBAS

La aplicación se ejecutó correctamente con todas las dependencias cargadas.

---

## 🚀 COMANDOS PARA EJECUTAR LA APLICACIÓN

### Opción 1: Ejecutar desde JAR (Recomendado para Producción)

```bash
cd c:\Users\Admin\Documents\GitHub\back-voley
java -jar target/galacticos-0.0.1-SNAPSHOT.jar
```

**Tiempo de inicio**: ~23 segundos  
**Puerto**: 8080  
**URL Base**: http://localhost:8080

### Opción 2: Ejecutar desde Maven (Desarrollo)

```bash
cd c:\Users\Admin\Documents\GitHub\back-voley
mvnw.cmd spring-boot:run
```

---

## 📝 ESTRUCTURA DEL ENDPOINT

### URL
```
POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
```

### Headers
```
Content-Type: multipart/form-data
```

### Parámetros

| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| `file` | File (multipart) | ✅ Sí | Archivo Excel (.xlsx) con estudiantes |
| `sedeId` | Query Parameter (Integer) | ✅ Sí | ID de la sede donde se registran los estudiantes |

### Content-Type
```
multipart/form-data
```

---

## 📄 ESTRUCTURA DEL ARCHIVO EXCEL

El archivo debe tener las siguientes columnas (en orden):

| # | Columna | Tipo | Requerido | Ejemplo |
|----|---------|------|-----------|---------|
| 1 | Nombre Completo | Texto | ✅ | Juan Pérez García |
| 2 | Tipo de Documento | Texto | ✅ | CC |
| 3 | Número de Documento | Número | ✅ | 1234567890 |
| 4 | Fecha de Nacimiento | Fecha | ✅ | 2005-06-15 |
| 5 | Edad | Número | ❌ | 18 |
| 6 | Sexo | Texto | ❌ | M/F |
| 7 | Dirección Residencia | Texto | ❌ | Calle 123 # 45-67 |
| 8 | Barrio | Texto | ❌ | Centro |
| 9 | Celular Estudiante | Texto | ❌ | 3101234567 |
| 10 | WhatsApp Estudiante | Texto | ❌ | 3101234567 |
| 11 | Correo Estudiante | Email | ✅ | estudiante@email.com |
| 12 | Nombre Sede | Texto | ❌ | Sede Principal |
| 13 | Nombre Tutor | Texto | ❌ | María García |
| 14 | Parentesco Tutor | Texto | ❌ | Madre |
| 15 | Documento Tutor | Número | ❌ | 9876543210 |
| 16 | Teléfono Tutor | Texto | ❌ | 3209876543 |
| 17 | Correo Tutor | Email | ❌ | tutor@email.com |
| 18 | Ocupación Tutor | Texto | ❌ | Profesional |
| 19 | Institución Educativa | Texto | ❌ | Colegio Central |
| 20 | Jornada | Texto | ❌ | Mañana |
| 21 | Grado Actual | Número | ❌ | 10 |
| 22 | EPS | Texto | ❌ | Sanitas |
| 23 | Tipo de Sangre | Texto | ❌ | O+ |
| 24 | Alergias | Texto | ❌ | Penicilina |
| 25 | Enfermedades/Condiciones | Texto | ❌ | Asma |
| 26 | Medicamentos | Texto | ❌ | Salbutamol |
| 27 | Certificado Médico Deportivo | Booleano | ❌ | SI/NO |
| 28 | Día Pago Mes | Número | ❌ | 15 |
| 29 | Nombre Emergencia | Texto | ❌ | Carlos Pérez |
| 30 | Teléfono Emergencia | Texto | ❌ | 3007654321 |
| 31 | Parentesco Emergencia | Texto | ❌ | Hermano |
| 32 | Ocupación Emergencia | Texto | ❌ | Ingeniero |
| 33 | Correo Emergencia | Email | ❌ | emergencia@email.com |
| 34 | Pertenece IGBTIQ | Booleano | ❌ | SI/NO |
| 35 | Persona con Discapacidad | Booleano | ❌ | SI/NO |
| 36 | Condición Discapacidad | Texto | ❌ | Movilidad reducida |
| 37 | Migrante/Refugiado | Booleano | ❌ | SI/NO |
| 38 | Población Étnica | Texto | ❌ | Indígena |
| 39 | Religión | Texto | ❌ | Católica |
| 40 | Experiencia en Voleibol | Texto | ❌ | Avanzada |
| 41 | Otras Disciplinas | Texto | ❌ | Fútbol |
| 42 | Posición Preferida | Texto | ❌ | Levantador |
| 43 | Dominancia | Texto | ❌ | Diestra |
| 44 | Nivel Actual | Texto | ❌ | Intermedio |
| 45 | Clubes Anteriores | Texto | ❌ | Galácticos |
| 46 | Consentimiento Informado | Booleano | ❌ | SI/NO |
| 47 | Firma Digital | Texto | ❌ | URL o base64 |
| 48 | Fecha Diligenciamiento | Fecha | ❌ | 2026-02-16 |

---

## 📤 RESPUESTA EXITOSA

### Código HTTP
```
200 OK
```

### Formato JSON
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
      "mensaje": "Estudiante importado correctamente",
      "idEstudiante": 1001,
      "email": "estudiante@email.com",
      "password": "1234567890"
    },
    {
      "fila": 3,
      "nombre": "María López Rodríguez",
      "estado": "ERROR",
      "mensaje": "El correo electronico ya existe en la base de datos",
      "idEstudiante": null,
      "email": "maria@email.com",
      "password": null
    }
  ]
}
```

### Campos de Respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `exitosos` | Integer | Número de estudiantes importados correctamente |
| `errores` | Integer | Número de estudiantes con errores |
| `total` | Integer | Total de filas procesadas |
| `resultados` | Array | Detalle de cada fila procesada |
| `resultados[].fila` | Integer | Número de fila en el Excel |
| `resultados[].nombre` | String | Nombre del estudiante |
| `resultados[].estado` | String | `EXITOSO` o `ERROR` |
| `resultados[].mensaje` | String | Detalles del resultado |
| `resultados[].idEstudiante` | Integer | ID generado (null si error) |
| `resultados[].email` | String | Email del usuario creado (null si error) |
| `resultados[].password` | String | Contraseña inicial: número de documento |

---

## ❌ RESPUESTAS DE ERROR

### Error 400 - Solicitud Inválida

```json
{
  "error": "Formato inválido",
  "detalles": "El archivo debe ser de tipo .xlsx (Excel 2007+)"
}
```

### Error 400 - Sede no válida

```json
{
  "error": "Sede no encontrada",
  "detalles": "El ID de sede especificado no existe"
}
```

### Error 413 - Archivo muy grande

```json
{
  "error": "Archivo demasiado grande",
  "detalles": "El archivo no puede exceder 5MB"
}
```

### Error 500 - Error del servidor

```json
{
  "error": "Error procesando Excel",
  "detalles": "Error inesperado al procesar el archivo"
}
```

---

## 🧪 PRUEBA CON CURL

### Comando básico

```bash
curl -X POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1 \
  -F "file=@estudiantes.xlsx"
```

### Con headers personalizados

```bash
curl -X POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1 \
  -H "Accept: application/json" \
  -F "file=@estudiantes.xlsx" \
  -v
```

---

## 🧪 PRUEBA CON POSTMAN

### Steps:

1. **Crear nueva petición**
   - Método: `POST`
   - URL: `http://localhost:8080/api/estudiantes/importar-excel?sedeId=1`

2. **Headers** (auto-configurados por multipart)
   - Content-Type: multipart/form-data

3. **Body - form-data**
   - Key: `file`
   - Type: `File`
   - Value: (seleccionar archivo Excel)

4. **Send**

5. **Ver respuesta en tab Body (JSON)**

---

## 🧪 PRUEBA CON ANGULAR

```typescript
// en el componente
iniciarImportacion(): void {
  const archivo = this.archivoSeleccionado;
  const sedeId = this.sedeSeleccionada;

  this.excelImportService.importarEstudiantesDesdeExcel(archivo, sedeId)
    .subscribe({
      next: (respuesta) => {
        console.log('Importación exitosa:', respuesta);
        // Mostrar resultados
      },
      error: (error) => {
        console.error('Error:', error);
        // Mostrar mensaje de error
      }
    });
}
```

---

## ✅ CASOS DE PRUEBA

### Caso 1: Importación exitosa

```
Archivo: estudiantes_validos.xlsx (25 registros válidos)
Sede: 1
Resultado esperado: 25 exitosos, 0 errores
```

### Caso 2: Importación con algunos errores

```
Archivo: estudiantes_mixtos.xlsx (27 registros, 2 con correos duplicados)
Sede: 1
Resultado esperado: 25 exitosos, 2 errores
```

### Caso 3: Archivo inválido

```
Archivo: documentos.pdf
Resultado esperado: Error 400 - Formato inválido
```

### Caso 4: Sede no existe

```
Archivo: estudiantes.xlsx
Sede: 999
Resultado esperado: Error 400 - Sede no encontrada
```

---

## 🔐 CONSIDERACIONES DE SEGURIDAD

✅ **Implementado**:
- Validación de tipo de archivo (.xlsx)
- Validación de tamaño máximo (5MB)
- Sanitización de datos de entrada
- Validación de campos requeridos
- Encriptación de contraseñas generadas
- Transacciones ACID por estudiante
- Logs de auditoría por importación

---

## 📊 MONITOREO

### Logs a verificar

```bash
# Inicialización
grep "ExcelImportService" logs.txt

# Importación
grep "Importando" logs.txt

# Errores
grep "ERROR" logs.txt
```

### Métricas de éxito

- `exitosos > 0` ✅
- `errores == 0` (ideal) ✅
- `total == exitosos + errores` ✅

---

## 📚 REFERENCIAS

- [Documentación Especificaciones Frontend](./ESPECIFICACIONES_FRONTEND_ANGULAR_17.md)
- [Fix de Error de Dependencias](./FIX_RUNTIME_ERROR_POI.md)
- [API REST Documentación](./API_REST_DOCUMENTACION.md)

---

**Estado**: LISTO PARA PRUEBAS ✅  
**Última actualización**: 16 de Febrero de 2026  
**Versión**: 1.0
