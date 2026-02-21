# 🧪 Testing - Importación de Estudiantes

## 🔍 Casos de Prueba

### Caso 1: Flujo Feliz (Happy Path)
```
✅ ESCENARIO: Importación exitosa de 3 estudiantes
Precondición: Plantilla descargada y completada correctamente
Pasos:
  1. Seleccionar sede
  2. Descargar plantilla
  3. Completar datos de 3 estudiantes
  4. Cargar archivo
  5. Confirmar importación
Resultado esperado:
  ✓ 3 exitosos
  ✓ 0 errores
  ✓ Mensaje de éxito en cada fila
```

### Caso 2: Archivo Inválido
```
❌ ESCENARIO: Intentar cargar archivo no Excel
Pasos:
  1. Seleccionar archivo .pdf o .txt
Resultado esperado:
  ✓ Mensaje de error: "Solo se aceptan archivos .xlsx"
  ✓ Archivo rechazado
```

### Caso 3: Archivo Muy Grande
```
❌ ESCENARIO: Archivo mayor a 10MB
Pasos:
  1. Intentar cargar archivo > 10MB
Resultado esperado:
  ✓ Mensaje de error con tamaño actual
  ✓ Archivo rechazado
```

### Caso 4: Campo Requerido Faltante
```
❌ ESCENARIO: Fila sin nombre
Datos:
  - Nombre Completo: [VACÍO]
  - Tipo Documento: Cédula
  - Numero: 1234567890
  - Fecha: 21/11/2001
  - Correo: test@test.com
Resultado esperado:
  ✓ Fila marcada como error
  ✓ Mensaje: "Nombre completo requerido"
```

### Caso 5: Email Duplicado
```
❌ ESCENARIO: Dos estudiantes con mismo correo
Datos:
  Estudiante 1: email@test.com
  Estudiante 2: email@test.com
Resultado esperado:
  ✓ Primer estudiante: exitoso
  ✓ Segundo estudiante: error
  ✓ Mensaje: "Correo ya registrado"
```

### Caso 6: Fecha Inválida
```
❌ ESCENARIO: Formato de fecha incorrecto
Datos:
  - Fecha Nacimiento: "21-11-2001" (en lugar de 21/11/2001)
Resultado esperado:
  ✓ Fila marcada como error
  ✓ Mensaje: "Formato de fecha inválido (esperado DD/MM/YYYY)"
```

### Caso 7: Documento Duplicado
```
❌ ESCENARIO: Dos estudiantes con mismo documento
Datos:
  Estudiante 1: 1234567890
  Estudiante 2: 1234567890
Resultado esperado:
  ✓ Primer estudiante: exitoso
  ✓ Segundo estudiante: error
  ✓ Mensaje: "Documento ya registrado"
```

---

## 🔗 Endpoints para Pruebas

### 1. Descargar Plantilla
```bash
GET /api/estudiantes/descargar-plantilla
```
**Respuesta esperada:**
- Status: 200
- Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Body: Archivo .xlsx descargable

**Prueba con cURL:**
```bash
curl -X GET http://localhost:8080/api/estudiantes/descargar-plantilla \
  -o plantilla-descargada.xlsx
```

### 2. Importar Estudiantes
```bash
POST /api/estudiantes/importar-excel?sedeId=1
Content-Type: multipart/form-data

file: <archivo.xlsx>
```

**Respuesta exitosa:**
```json
{
  "exitosos": 3,
  "errores": 0,
  "total": 3,
  "timestamp": "2026-02-20T11:30:00",
  "resultados": [
    {
      "fila": 2,
      "nombreEstudiante": "Juan Pérez García",
      "numeroDocumento": "1234567890",
      "estado": "exitoso",
      "email": "juan.perez@example.com",
      "password": "TempPass123!@"
    },
    {
      "fila": 3,
      "nombreEstudiante": "María López Rodríguez",
      "numeroDocumento": "9876543210",
      "estado": "exitoso",
      "email": "maria.lopez@example.com",
      "password": "TempPass456!@"
    },
    {
      "fila": 4,
      "nombreEstudiante": "Carlos Gómez Martínez",
      "numeroDocumento": "5555555555",
      "estado": "exitoso",
      "email": "carlos.gomez@example.com",
      "password": "TempPass789!@"
    }
  ]
}
```

**Respuesta con errores:**
```json
{
  "exitosos": 1,
  "errores": 2,
  "total": 3,
  "timestamp": "2026-02-20T11:30:00",
  "resultados": [
    {
      "fila": 2,
      "nombreEstudiante": "Juan Pérez García",
      "numeroDocumento": "1234567890",
      "estado": "exitoso",
      "email": "juan.perez@example.com",
      "password": "TempPass123!@"
    },
    {
      "fila": 3,
      "nombreEstudiante": "María López",
      "numeroDocumento": "9876543210",
      "estado": "error",
      "mensaje": "Correo electrónico requerido"
    },
    {
      "fila": 4,
      "nombreEstudiante": "",
      "numeroDocumento": "5555555555",
      "estado": "error",
      "mensaje": "Nombre completo requerido"
    }
  ]
}
```

**Prueba con cURL:**
```bash
curl -X POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1 \
  -F "file=@plantilla-completada.xlsx"
```

**Prueba con Postman:**
```
1. Seleccionar método POST
2. URL: http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
3. Tab "Body" → seleccionar "form-data"
4. Key: "file" | Value: seleccionar archivo .xlsx
5. Click Send
```

---

## 📋 Datos de Prueba (JSON)

Estos datos pueden ser importados para test:

```json
{
  "estudiantes": [
    {
      "nombreCompleto": "Juan Pérez García",
      "tipoDocumento": "Cédula",
      "numeroDocumento": "1234567890",
      "fechaNacimiento": "21/11/2001",
      "edad": 25,
      "sexo": "Masculino",
      "direccionResidencia": "Calle 10 #20-30",
      "barrio": "Centro",
      "celularEstudiante": "3001234567",
      "whatsappEstudiante": "3001234567",
      "correoEstudiante": "juan.perez@example.com",
      "nombreTutor": "Maria García",
      "parentescoTutor": "Madre",
      "documentoTutor": "1098765432",
      "telefonoTutor": "3109876543",
      "correoTutor": "maria@example.com",
      "ocupacionTutor": "Docente",
      "institucionEducativa": "Colegio XYZ",
      "jornada": "Matutina",
      "gradoActual": "10",
      "eps": "EPS Salud",
      "tipoSangre": "O+",
      "alergias": "Ninguna",
      "enfermedadesCondiciones": "Ninguna",
      "medicamentos": "Ninguno",
      "certificadoMedicoDeportivo": "No",
      "diaPagoMes": 15,
      "nombreEmergencia": "Carlos García",
      "telefonoEmergencia": "3101234567",
      "parentescoEmergencia": "Abuelo",
      "ocupacionEmergencia": "Jubilado",
      "correoEmergencia": "carlos@example.com",
      "perteneceIgbtiq": "No",
      "personaDiscapacidad": "No",
      "condicionDiscapacidad": "Ninguna",
      "migranteRefugiado": "No",
      "poblacionEtnica": "Ninguna",
      "religion": "Católica",
      "experienciaVoleibol": "Intermedio",
      "otrasDisciplinas": "Futbol",
      "posicionPreferida": "Zaguera",
      "dominancia": "Diestro",
      "nivelActual": "Intermedio",
      "clubesAnteriores": "Club Deportivo"
    },
    {
      "nombreCompleto": "María López Rodríguez",
      "tipoDocumento": "Cédula",
      "numeroDocumento": "9876543210",
      "fechaNacimiento": "15/03/2002",
      "edad": 22,
      "sexo": "Femenino",
      "direccionResidencia": "Carrera 5 #15-40",
      "barrio": "Nororiental",
      "celularEstudiante": "3009876543",
      "whatsappEstudiante": "3009876543",
      "correoEstudiante": "maria.lopez@example.com",
      "nombreTutor": "Juan López",
      "parentescoTutor": "Padre",
      "documentoTutor": "1087654321",
      "telefonoTutor": "3107654321",
      "correoTutor": "juan@example.com",
      "ocupacionTutor": "Ingeniero",
      "institucionEducativa": "Instituto ABC",
      "jornada": "Vespertina",
      "gradoActual": "11",
      "eps": "EPS Plus",
      "tipoSangre": "AB-",
      "alergias": "Polen",
      "enfermedadesCondiciones": "Ninguna",
      "medicamentos": "Ninguno",
      "certificadoMedicoDeportivo": "No",
      "diaPagoMes": 10,
      "nombreEmergencia": "Rosa López",
      "telefonoEmergencia": "3108765432",
      "parentescoEmergencia": "Abuela",
      "ocupacionEmergencia": "Ama de casa",
      "correoEmergencia": "rosa@example.com",
      "perteneceIgbtiq": "No",
      "personaDiscapacidad": "No",
      "condicionDiscapacidad": "Ninguna",
      "migranteRefugiado": "No",
      "poblacionEtnica": "Ninguna",
      "religion": "Protestante",
      "experienciaVoleibol": "Avanzado",
      "otrasDisciplinas": "Natación",
      "posicionPreferida": "Levantadora",
      "dominancia": "Zurda",
      "nivelActual": "Avanzado",
      "clubesAnteriores": "Club Acuático"
    }
  ]
}
```

---

## 🐛 Debugging

### Problema: No se descarga la plantilla

**Solución:**
```
1. Verificar que el backend esté corriendo: http://localhost:8080
2. Revisar consola del navegador (F12 → Network)
3. Verificar que el endpoint GET /descargar-plantilla retorne 200
4. Revisar CORS en backend si está en otro dominio
```

### Problema: Error al importar (CORS)

**Solución en Backend:**
```java
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController { ... }
```

### Problema: Archivo rechazado

**Verificar:**
```
- ¿Es extensión .xlsx?
- ¿Archivo < 10MB?
- ¿Archivo no corrupto?
```

### Problema: Respuesta vacía de importación

**Revisar:**
```
- ¿sedeId es válido?
- ¿Existe la sede en BD?
- ¿Archivo Excel está correctamente formado?
```

---

## ✅ Checklist de Testing

- [ ] Descargar plantilla correctamente
- [ ] Archivo descargado contiene 44 columnas
- [ ] Archivo incluye 3 ejemplos
- [ ] Cargar archivo válido
- [ ] Cargar archivo .pdf rechazado
- [ ] Cargar archivo > 10MB rechazado
- [ ] Importación exitosa con 3 estudiantes
- [ ] Campo requerido faltante genera error
- [ ] Email duplicado genera error
- [ ] Documento duplicado genera error
- [ ] Fecha inválida genera error
- [ ] Respuesta muestra detalles correctos
- [ ] Tabla de resultados se carga
- [ ] Botón volver funciona
- [ ] Descargar reporte funciona

---

## 📊 Monitoreo de Performance

```
Métrica esperada: < 500ms para importar 100 estudiantes

Para medir:
1. Abrir DevTools (F12)
2. Tab Network
3. Filtrar por importar-excel
4. Revisar tiempo de respuesta
```

---

## 🔐 Validaciones de Seguridad

```
✓ Archivo validado en frontend (extensión, tamaño)
✓ Archivo validado en backend (extensión, tamaño, estructura)
✓ Datos sanitizados antes de guardar en BD
✓ SQL Injection previsto con prepared statements
✓ Validación de autorización en endpoint
✓ Logs de todas las importaciones
```

