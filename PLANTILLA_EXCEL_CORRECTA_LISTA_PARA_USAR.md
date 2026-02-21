# 📋 PLANTILLA EXCEL CORRECTA - LISTA PARA USAR

**¡Esta es la estructura EXACTA que tu Excel debe tener!**

---

## 📊 ESTRUCTURA PERFECTA

### Encabezados (FILA 1) - OBLIGATORIO

```
Celda A1: nombreCompleto
Celda B1: tipoDocumento
Celda C1: numeroDocumento
Celda D1: fechaNacimiento
Celda E1: correoEstudiante
```

**⚠️ IMPORTANTE:** Los encabezados EXACTAMENTE así, sin espacios adicionales al inicio/final

---

## 📝 EJEMPLO CON DATOS (Copia esto a tu Excel)

```
FILA 1 (ENCABEZADOS):
A1: nombreCompleto     | B1: tipoDocumento | C1: numeroDocumento | D1: fechaNacimiento | E1: correoEstudiante

FILA 2 (Estudiante 1):
A2: Juan Pérez García  | B2: Cédula        | C2: 37216000001     | D2: 21/11/2001      | E2: juan.perez@galactica.edu

FILA 3 (Estudiante 2):
A3: María López Díaz   | B3: Cédula        | C3: 37216000002     | D3: 15/03/2002      | E3: maria.lopez@galactica.edu

FILA 4 (Estudiante 3):
A4: Carlos Gómez       | B4: Cédula        | C4: 37216000003     | D4: 10/07/2001      | E4: carlos.gomez@galactica.edu

FILA 5 (Estudiante 4):
A5: Ana García López   | B5: Cédula        | C5: 37216000004     | D5: 08/05/2003      | E5: ana.garcia@galactica.edu

FILA 6 (Estudiante 5):
A6: Luis Fernando Rodr | B6: Cédula        | C6: 37216000005     | D6: 12/09/2000      | E6: luis.rodriguez@galactica.edu
```

---

## 🛠️ PASOS PARA CREAR EN EXCEL

### 1️⃣ Abre Excel o Google Sheets

### 2️⃣ Fila 1 - Escribe los encabezados exactamente:

| A | B | C | D | E |
|---|---|---|---|---|
| nombreCompleto | tipoDocumento | numeroDocumento | fechaNacimiento | correoEstudiante |

### 3️⃣ Fila 2+ - Escribe tus datos

| A | B | C | D | E |
|---|---|---|---|---|
| Juan Pérez García | Cédula | 37216000001 | 21/11/2001 | juan.perez@galactica.edu |
| María López | Cédula | 37216000002 | 15/03/2002 | maria.lopez@galactica.edu |

### 4️⃣ Configurar formato de fecha (IMPORTANTE)

1. Selecciona columna D (fechaNacimiento)
2. Click derecho → Formato de celdas
3. Tipo: **Fecha**
4. Formato: **DD/MM/YYYY**
5. OK

### 5️⃣ Guarda como Excel

- Excel: Archivo → Guardar como → **Formato .xlsx**
- Google Sheets: Descargar → **Microsoft Excel**

---

## ✅ CHECKLIST ANTES DE IMPORTAR

- [ ] ¿Los encabezados están en FILA 1?
  - A1: `nombreCompleto` (sin acentos extra, sin espacios)
  - B1: `tipoDocumento`
  - C1: `numeroDocumento`
  - D1: `fechaNacimiento`
  - E1: `correoEstudiante`

- [ ] ¿Los datos comienzan en FILA 2?

- [ ] ¿NO hay filas vacías en medio?

- [ ] ¿Columna A (nombres)**: NO está vacía?

- [ ] ¿Columna B (tipo doc)**: NO está vacía?

- [ ] ¿Columna C (número doc)**: NO está vacía?

- [ ] ¿Columna D (fecha)**: En formato DD/MM/YYYY (ej: 21/11/2001)?

- [ ] ¿Columna E (email)**: NO está vacía?

- [ ] ¿El archivo es .xlsx?

- [ ] ¿Tamaño ≤ 10MB?

---

## ❌ ERRORES A EVITAR

### ❌ INCORRECTO - Datos desalineados
```
A        │ B       │ C      │ D       │ E
Cédula   │ 37216   │ (vacío)│ (vacío) │ (vacío) ← MAL
```

### ✅ CORRECTO - Datos en columnas correctas
```
A                │ B      │ C        │ D         │ E
nombreCompleto   │ tipo   │ número   │ fecha     │ email
Juan Pérez García│ Cédula │ 37216    │ 21/11/01  │ j@ex.com
```

---

## 🔄 CÓMO ARREGLAR TU EXCEL ACTUAL

**Tu Excel actual tiene el problema:**
```
Fila 1: (vacío) | Cédula | 37216 | (vacío) | (vacío)
Fila 2: (vacío) | (vacío)| (vacío)| (vacío) | (vacío)
```

**Debe ser:**
```
Fila 1: nombreCompleto | tipoDocumento | numeroDocumento | fechaNacimiento | correoEstudiante
Fila 2: Juan Pérez     | Cédula        | 37216           | 21/11/2001      | juan@ex.com
```

---

## 🚀 SOLUCIÓN RÁPIDA

1. **Abre tu Excel**
2. **Borra TODO el contenido**
3. **Copia esta tabla:**

```
nombreCompleto | tipoDocumento | numeroDocumento | fechaNacimiento | correoEstudiante
Juan Pérez García | Cédula | 1234567890 | 21/11/2001 | juan@example.com
María López Rodríguez | Cédula | 9876543210 | 15/03/2002 | maria@example.com
Carlos Gómez Martínez | Cédula | 5555555555 | 10/07/2001 | carlos@example.com
```

4. **Pega en Excel comenzando en A1**
5. **Guarda como .xlsx**
6. **Sube a la aplicación**

---

## ✨ RESULTADO ESPERADO

Cuando importes con esta estructura correcta:

```
✅ Fila 2: "Juan Pérez García" → EXITOSO
✅ Fila 3: "María López Rodríguez" → EXITOSO
✅ Fila 4: "Carlos Gómez Martínez" → EXITOSO

Respuesta: "3 exitosos, 0 errores"
```

---

## 📞 SI SIGUE FALLANDO

**Revisa:**
1. ¿Los encabezados están EXACTAMENTE así?
   - `nombreCompleto` (no `Nombre Completo`, no `nombre_completo`)
   - `tipoDocumento` (no `tipo_documento`, no `Tipo Documento`)
   - `numeroDocumento` (no `numero_documento`, no `Número Documento`)
   - `fechaNacimiento` (no `fecha_nacimiento`, no `Fecha Nacimiento`)
   - `correoEstudiante` (no `email`, no `Correo Electrónico`)

2. ¿No hay espacios antes/después de los encabezados?

3. ¿Los datos están en las columnas A-E?

4. ¿La fecha está en DD/MM/YYYY?

5. ¿El archivo es .xlsx?

**Si aún falla:** Descarga de nuevo desde el sistema una plantilla en blanco y llena los datos cuidadosamente.

---

## 🎯 TU PRÓXIMO PASO

1. Crea un Excel con la estructura correcta
2. Llena 3-5 filas de datos
3. Sube a la aplicación
4. ✅ Verás "3-5 exitosos, 0 errores"

**¡Listo! Ya puedes importar cientos de estudiantes.**
