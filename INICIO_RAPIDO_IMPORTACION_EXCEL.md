# ⚡ INICIO RÁPIDO - IMPORTACIÓN DE EXCEL

## 🎯 OBJETIVO
Importar masivamente estudiantes desde Excel y crear automáticamente usuarios con credenciales.

---

## ⏱️ 5 MINUTOS PARA EMPEZAR

### 1. Verificar que la aplicación está corriendo

```bash
# Terminal en c:\Users\Admin\Documents\GitHub\back-voley
java -jar target/galacticos-0.0.1-SNAPSHOT.jar
```

**Resultado esperado**:
```
Tomcat started on port 8080 (http) with context path '/'
```

### 2. Crear archivo Excel

**Nombre**: `estudiantes.xlsx`

**Estructura** (mínimo 11 columnas):
```
| Nombre Completo | Tipo Doc | Documento | F. Nac | Sexo | ... | Email | ... |
|---|---|---|---|---|---|---|---|
| Juan Pérez | CC | 1234567890 | 2005-06-15 | M | ... | juan@email.com | ... |
| María López | CC | 0987654321 | 2006-03-20 | F | ... | maria@email.com | ... |
```

**Campos requeridos**:
1. Nombre Completo
2. Tipo de Documento
3. Número de Documento
4. Fecha de Nacimiento
5. Correo Estudiante

### 3. Llamar al endpoint

**URL**:
```
POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
```

**Con CURL**:
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -F "file=@estudiantes.xlsx"
```

**Con Postman**:
1. Método: POST
2. URL: http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
3. Body → form-data → file (tipo File)
4. Send

### 4. Ver respuesta

```json
{
  "exitosos": 2,
  "errores": 0,
  "total": 2,
  "resultados": [
    {
      "fila": 2,
      "nombre": "Juan Pérez",
      "estado": "EXITOSO",
      "mensaje": "Estudiante importado correctamente",
      "idEstudiante": 1001,
      "email": "juan@email.com",
      "password": "1234567890"
    }
  ]
}
```

### 5. Credenciales creadas automáticamente

```
Email: juan@email.com
Password: 1234567890 (número de documento)
```

---

## 📋 CAMPOS REQUERIDOS vs OPCIONALES

### ✅ Requeridos (5 campos)
- Nombre Completo
- Tipo de Documento
- Número de Documento
- Fecha de Nacimiento
- Correo Estudiante

### ❌ Opcionales (43 campos)
- Todos los demás campos del Excel
- Si no están presentes, se dejan en blanco

---

## 🎨 PLANTILLA DE EXCEL

Descargar o crear con estas columnas en este orden:

```
A: Nombre Completo
B: Tipo de Documento
C: Número de Documento
D: Fecha de Nacimiento
E: Edad
F: Sexo
G: Dirección Residencia
H: Barrio
I: Celular Estudiante
J: WhatsApp Estudiante
K: Correo Estudiante
L: Nombre Sede
M: Nombre Tutor
N: Parentesco Tutor
O: Documento Tutor
P: Teléfono Tutor
Q: Correo Tutor
R: Ocupación Tutor
S: Institución Educativa
T: Jornada
U: Grado Actual
V: EPS
W: Tipo de Sangre
X: Alergias
Y: Enfermedades/Condiciones
Z: Medicamentos
AA: Certificado Médico Deportivo
AB: Día Pago Mes
AC: Nombre Emergencia
AD: Teléfono Emergencia
AE: Parentesco Emergencia
AF: Ocupación Emergencia
AG: Correo Emergencia
AH: Pertenece IGBTIQ
AI: Persona con Discapacidad
AJ: Condición Discapacidad
AK: Migrante/Refugiado
AL: Población Étnica
AM: Religión
AN: Experiencia en Voleibol
AO: Otras Disciplinas
AP: Posición Preferida
AQ: Dominancia
AR: Nivel Actual
AS: Clubes Anteriores
AT: Consentimiento Informado
AU: Firma Digital
AV: Fecha Diligenciamiento
```

---

## ❌ ERRORES COMUNES

### Error: "El archivo debe ser .xlsx"
- **Causa**: Archivo en formato .xls o .csv
- **Solución**: Convertir a Excel 2007+ (.xlsx)

### Error: "Sede no encontrada"
- **Causa**: sedeId no existe
- **Solución**: Usar un sedeId válido existente en la BD

### Error: "El correo electrónico ya existe"
- **Causa**: Email duplicado en BD
- **Solución**: Usar emails únicos en el Excel

### Error: "Campos requeridos incompletos"
- **Causa**: Faltan datos en columnas obligatorias
- **Solución**: Verificar que todas las 5 columnas requeridas tengan datos

---

## 📊 RESPUESTAS DE ERROR

### 400 - Bad Request
```json
{
  "error": "Sede no encontrada",
  "detalles": "El ID de sede especificado no existe"
}
```

### 413 - Archivo muy grande
```json
{
  "error": "Archivo demasiado grande",
  "detalles": "El archivo no puede exceder 5MB"
}
```

### 500 - Error interno
```json
{
  "error": "Error procesando Excel",
  "detalles": "Error inesperado al procesar el archivo"
}
```

---

## 📚 DOCUMENTACIÓN COMPLETA

Para más información, consultar:

1. **ESPECIFICACIONES_FRONTEND_ANGULAR_17.md** - Implementación Angular
2. **GUIA_TESTING_ENDPOINT_EXCEL.md** - Casos de prueba detallados
3. **RESUMEN_FINAL_IMPORTACION_EXCEL.md** - Resumen ejecutivo
4. **INDICE_COMPLETO_IMPORTACION_EXCEL.md** - Índice de archivos

---

## 🚀 PRÓXIMOS PASOS

### Frontend (Angular)
```bash
npm install
ng generate component components/importar-estudiantes
# Copiar código de ESPECIFICACIONES_FRONTEND_ANGULAR_17.md
ng serve
```

### Testing
```bash
# Probar endpoint
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -F "file=@estudiantes.xlsx"
```

### Producción
```bash
java -jar target/galacticos-0.0.1-SNAPSHOT.jar
```

---

## ✨ CARACTERÍSTICAS

✅ Importa múltiples estudiantes  
✅ Crea usuarios automáticamente  
✅ Genera credenciales de acceso  
✅ Valida consistencia de datos  
✅ Reporte detallado de resultados  
✅ Transacciones atómicas  
✅ Manejo robusto de errores  

---

## 📞 SOPORTE RÁPIDO

| Problema | Solución |
|----------|----------|
| App no inicia | `mvnw clean install` |
| Error de dependencias | `mvnw clean install -DskipTests` |
| Endpoint no responde | Verificar que port 8080 esté disponible |
| Errores de BD | Verificar conexión MySQL |

---

**¡Listo para empezar! 🎉**

Tiempo estimado: 5 minutos
