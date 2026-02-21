# PLANTILLA EXCEL PARA IMPORTACIÓN DE ESTUDIANTES

## Instrucciones:
1. Descarga este archivo y guárdalo como `plantilla-estudiantes.xlsx`
2. Llena los datos siguiendo la estructura exactamente
3. NO cambies los nombres de las columnas (fila 1)
4. NO dejes filas vacías
5. Asegúrate que las FECHAS estén en formato DD/MM/YYYY

## Estructura de Datos (copia y pega en Excel):

```
FILA 1 (ENCABEZADOS - NO MODIFICAR):
nombreCompleto | tipoDocumento | numeroDocumento | fechaNacimiento | correoEstudiante

EJEMPLOS DE DATOS VÁLIDOS:

FILA 2:
Juan Pérez García | Cédula | 1234567890 | 21/11/2001 | juan.perez@example.com

FILA 3:
María López Rodríguez | Cédula | 9876543210 | 15/03/2002 | maria.lopez@example.com

FILA 4:
Carlos Gómez Martínez | Cédula | 5555555555 | 10/07/2001 | carlos.gomez@example.com

FILA 5:
Ana María García López | Cédula | 2222222222 | 08/05/2003 | ana.garcia@example.com

FILA 6:
Luis Fernando Rodríguez | Cédula | 7777777777 | 12/09/2000 | luis.rodriguez@example.com

FILA 7:
Camila González Díaz | Cédula | 4444444444 | 25/01/2002 | camila.gonzalez@example.com

FILA 8:
Roberto Martínez Silva | Cédula | 8888888888 | 30/06/2001 | roberto.martinez@example.com

FILA 9:
Sofía López Ramírez | Cédula | 3333333333 | 14/12/2002 | sofia.lopez@example.com
```

## ⚠️ ERRORES A EVITAR:

❌ INCORRECTO - Fecha con guiones:
    Ana García | Cédula | 1111111111 | 2001-11-21 | ana@example.com

✅ CORRECTO - Fecha con barras:
    Ana García | Cédula | 1111111111 | 21/11/2001 | ana@example.com

---

❌ INCORRECTO - Fila vacía:
    Juan Pérez | ... | ... | ... | ...
    [FILA VACÍA] ← PROBLEMA
    María López | ... | ... | ... | ...

✅ CORRECTO - Sin filas vacías:
    Juan Pérez | ... | ... | ... | ...
    María López | ... | ... | ... | ...

---

❌ INCORRECTO - Email duplicado:
    Juan García | Cédula | 1234567890 | 21/11/2001 | juan@example.com
    Juan Dos | Cédula | 9999999999 | 15/03/2002 | juan@example.com ← FALLA

✅ CORRECTO - Emails únicos:
    Juan García | Cédula | 1234567890 | 21/11/2001 | juan@example.com
    Juan Dos | Cédula | 9999999999 | 15/03/2002 | juandos@example.com

---

❌ INCORRECTO - Documento duplicado:
    Juan García | Cédula | 1234567890 | 21/11/2001 | juan@example.com
    Juan Dos | Cédula | 1234567890 | 15/03/2002 | juandos@example.com ← FALLA (mismo documento)

✅ CORRECTO - Documentos únicos:
    Juan García | Cédula | 1234567890 | 21/11/2001 | juan@example.com
    Juan Dos | Cédula | 9999999999 | 15/03/2002 | juandos@example.com
```

## 📋 Pasos para crear la plantilla en Excel:

1. **Abre Excel o Google Sheets**
2. **Fila 1**: Escribe exactamente estos encabezados en las columnas:
   - A1: `nombreCompleto`
   - B1: `tipoDocumento`
   - C1: `numeroDocumento`
   - D1: `fechaNacimiento`
   - E1: `correoEstudiante`

3. **Fila 2 en adelante**: Llena con tus datos reales siguiendo el patrón anterior

4. **Formato de fecha en Excel**:
   - Selecciona columna D (fechaNacimiento)
   - Click derecho → Formato de celdas
   - Tipo: Fecha
   - Formato: DD/MM/YYYY o simplemente escribe: 21/11/2001

5. **Guarda como**: 
   - Excel: Archivo → Guardar como → Formato .xlsx
   - Google Sheets: Descargar → Microsoft Excel

6. **Verifica antes de subir**:
   - ✅ No hay filas vacías
   - ✅ Fechas en DD/MM/YYYY
   - ✅ Todos los campos llenos
   - ✅ Emails válidos
   - ✅ Documentos sin duplicados

## 🚀 Listo para importar

Una vez guardado el archivo, úsalo en el frontend:

1. Selecciona el archivo .xlsx
2. Haz click en "Importar"
3. Espera a que se complete
4. Verifica los resultados

¡Listo! Tus estudiantes estarán registrados en el sistema.
