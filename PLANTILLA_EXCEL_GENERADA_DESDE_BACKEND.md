# Generador de Plantilla Excel desde Backend

## ✅ Implementación Completada

El backend ahora genera automáticamente la plantilla de Excel con todas las validaciones y ejemplos de datos.

## 📋 Cambios Realizados

### 1. **ExcelImportService.java**
- ✅ Agregado método `generarPlantillaExcel()` 
- ✅ Genera archivo Excel con 44 columnas exactas según especificación
- ✅ Headers resaltados en azul
- ✅ 3 ejemplos de estudiantes con datos completos
- ✅ Retorna `byte[]` para descarga directa

### 2. **EstudianteController.java**
- ✅ Inyectado `ExcelImportService`
- ✅ Nuevo endpoint: `GET /api/estudiantes/descargar-plantilla`
- ✅ Content-Type configurado correctamente
- ✅ Nombre de archivo: `plantilla-estudiantes-YYYY-MM-DD.xlsx`

## 🔗 Endpoint Disponible

```
GET /api/estudiantes/descargar-plantilla
```

**Descripción:** Descarga la plantilla de Excel con ejemplos para importar estudiantes

**Respuesta:**
- Status: 200 OK
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Body: Archivo Excel descargable

**Ejemplo cURL:**
```bash
curl -X GET http://localhost:8080/api/estudiantes/descargar-plantilla \
  -o plantilla-estudiantes.xlsx
```

## 📊 Estructura de la Plantilla

### 44 Columnas Exactas:

1. **Nombre Completo*** (requerido)
2. **Tipo Documento*** (requerido)
3. **Numero Documento*** (requerido)
4. **Fecha Nacimiento (DD/MM/YYYY)*** (requerido)
5. Edad
6. Sexo
7. Direccion Residencia
8. Barrio
9. Celular Estudiante
10. WhatsApp Estudiante
11. **Correo Estudiante*** (requerido)
12. Nombre Tutor
13. Parentesco Tutor
14. Documento Tutor
15. Telefono Tutor
16. Correo Tutor
17. Ocupacion Tutor
18. Institucion Educativa
19. Jornada
20. Grado Actual
21. EPS
22. Tipo Sangre
23. Alergias
24. Enfermedades/Condiciones
25. Medicamentos
26. Certificado Medico Deportivo (Si/No)
27. Dia Pago Mes
28. Nombre Emergencia
29. Telefono Emergencia
30. Parentesco Emergencia
31. Ocupacion Emergencia
32. Correo Emergencia
33. Pertenece LGBTIQ (Si/No)
34. Persona Discapacidad (Si/No)
35. Condicion Discapacidad
36. Migrante/Refugiado (Si/No)
37. Poblacion Etnica
38. Religion
39. Experiencia Voleibol
40. Otras Disciplinas
41. Posicion Preferida
42. Dominancia
43. Nivel Actual
44. Clubes Anteriores

**\* Campos requeridos para la importación**

## 📄 Datos de Ejemplo Incluidos

La plantilla incluye 3 ejemplos completos:

### Ejemplo 1: Juan Pérez García
- Cédula: 1234567890
- Correo: juan.perez@example.com
- Tutor: Maria García (Madre)
- Institución: Colegio XYZ, Grado 10, Matutina
- Deportes: Intermedio en Voleibol, Futbol, Club Deportivo

### Ejemplo 2: María López Rodríguez
- Cédula: 9876543210
- Correo: maria.lopez@example.com
- Tutor: Juan López (Padre)
- Institución: Instituto ABC, Grado 11, Vespertina
- Deportes: Avanzado en Voleibol, Natación, Club Acuático

### Ejemplo 3: Carlos Gómez Martínez
- Cédula: 5555555555
- Correo: carlos.gomez@example.com
- Tutor: Patricia Martínez (Madre)
- Institución: Liceo DEF, Grado 9, Única
- Deportes: Principiante en Voleibol, Tenis, Club de Tenis

## ✨ Validaciones Incluidas en la Importación

El sistema valida:

1. **Campos Requeridos:**
   - ✅ Nombre Completo (no vacío)
   - ✅ Tipo Documento (válido: Cédula, Pasaporte, etc.)
   - ✅ Numero Documento (único, formato válido)
   - ✅ Fecha Nacimiento (formato DD/MM/YYYY, edad válida)
   - ✅ Correo Estudiante (formato email válido)

2. **Campos Opcionales:**
   - ✓ Edad (calculada si no se proporciona)
   - ✓ Contacto tutor (al menos uno requerido)
   - ✓ Datos académicos
   - ✓ Información médica
   - ✓ Datos deportivos

3. **Formatos Esperados:**
   - Fechas: DD/MM/YYYY (ej: 21/11/2001)
   - Booleanos: Si/No (para: Certificado Médico, LGBTIQ, Discapacidad, Migrante)
   - Día de pago: Número 1-31
   - Emails: Formato email válido

## 🚀 Cómo Usar

### Paso 1: Descargar Plantilla
```bash
GET http://localhost:8080/api/estudiantes/descargar-plantilla
```

### Paso 2: Completar Datos
- Mantener los 3 ejemplos o eliminarlos
- Agregar nuevas filas con datos de estudiantes
- Respetar formatos de fechas y tipos de datos

### Paso 3: Importar Excel
```bash
POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
Content-Type: multipart/form-data

file: <archivo-excel>
```

## 📦 Archivos Modificados

```
src/main/java/galacticos_app_back/galacticos/service/ExcelImportService.java
├── + Agregado método generarPlantillaExcel()
├── + Importación ByteArrayOutputStream
└── + Lógica para crear workbook con headers y datos

src/main/java/galacticos_app_back/galacticos/controller/EstudianteController.java
├── + Inyección ExcelImportService
├── + Importación LocalDate
└── + Nuevo endpoint GET /descargar-plantilla
```

## 🔍 Verificación de Funcionamiento

### Compilación
```bash
mvnw.cmd clean compile
# ✅ SIN ERRORES
```

### Empaquetado
```bash
mvnw.cmd package -DskipTests
# ✅ JAR generado correctamente
```

### Ejecución
```bash
java -jar target/galacticos-0.0.1-SNAPSHOT.jar
# Esperando que inicie la aplicación...
```

### Prueba del Endpoint
```bash
curl -X GET http://localhost:8080/api/estudiantes/descargar-plantilla \
  -H "Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
  -o plantilla-descargada.xlsx
```

## 📝 Notas Importantes

- El endpoint NO requiere autenticación (GET público)
- La plantilla se genera dinámicamente cada vez que se solicita
- Los ejemplos son para referencia educativa
- Los datos de ejemplo pueden ser editados o eliminados
- El sistema acepta archivos Excel con cualquier número de filas

## ✅ Estado del Proyecto

- **Backend:** ✅ Implementado y compilado
- **Validaciones:** ✅ Incorporadas en ExcelImportService
- **Endpoint:** ✅ Disponible en /api/estudiantes/descargar-plantilla
- **Ejemplos:** ✅ 3 estudiantes completos incluidos
- **Próximo paso:** Iniciar aplicación y probar descargar plantilla

## 🎯 Beneficios

1. ✅ **Sin Frontend:** No depende del cliente para crear la plantilla
2. ✅ **Consistencia:** Todas las plantillas tienen exactamente los mismos campos
3. ✅ **Ejemplos:** Los usuarios ven datos reales para llenar el formulario
4. ✅ **Validación:** El backend valida antes de importar
5. ✅ **Mantenimiento:** Cambios en estructura solo requieren actualizar backend
6. ✅ **Escalabilidad:** Soporta cualquier número de estudiantes

