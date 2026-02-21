# 📥 PLANTILLA EXCEL - FORMATO PARA COPIAR Y PEGAR DIRECTAMENTE

## 🎯 USO RÁPIDO

1. **Abre Excel o Google Sheets**
2. **Selecciona celda A1**
3. **Copia TODO lo que está debajo**
4. **Pega en tu hoja (Ctrl+V)**
5. **Guarda como .xlsx**
6. **¡Sube a la aplicación!**

---

## 📋 TABLA LISTA PARA COPIAR

Copia desde aquí (desde la línea del encabezado) hasta el final:

```
nombreCompleto	tipoDocumento	numeroDocumento	fechaNacimiento	correoEstudiante
Juan Pérez García	Cédula	1001001001	21/11/2001	juan.perez@galactica.edu
María López Rodríguez	Cédula	1001001002	15/03/2002	maria.lopez@galactica.edu
Carlos Gómez Martínez	Cédula	1001001003	10/07/2000	carlos.gomez@galactica.edu
Ana García López	Cédula	1001001004	08/05/2003	ana.garcia@galactica.edu
Luis Fernando Rodríguez	Cédula	1001001005	12/09/2001	luis.rodriguez@galactica.edu
```

---

## 📝 CÓMO COPIAR CORRECTAMENTE

### Opción 1: Copia Todo (RECOMENDADO)

1. Selecciona desde `nombreCompleto` hasta `luis.rodriguez@galactica.edu`
2. Ctrl+C (Copiar)
3. Abre Excel
4. Haz clic en A1
5. Ctrl+V (Pegar)
6. ✅ Automáticamente se distribuye en columnas

### Opción 2: Copia desde línea de comando (PowerShell)

```powershell
@"
nombreCompleto	tipoDocumento	numeroDocumento	fechaNacimiento	correoEstudiante
Juan Pérez García	Cédula	1001001001	21/11/2001	juan.perez@galactica.edu
María López Rodríguez	Cédula	1001001002	15/03/2002	maria.lopez@galactica.edu
Carlos Gómez Martínez	Cédula	1001001003	10/07/2000	carlos.gomez@galactica.edu
Ana García López	Cédula	1001001004	08/05/2003	ana.garcia@galactica.edu
Luis Fernando Rodríguez	Cédula	1001001005	12/09/2001	luis.rodriguez@galactica.edu
"@ | Set-Clipboard
```

Luego pega en Excel: Ctrl+V

---

## 🎨 VISUALIZACIÓN EN EXCEL

Cuando lo pegues, verás esto:

| A | B | C | D | E |
|---|---|---|---|---|
| **nombreCompleto** | **tipoDocumento** | **numeroDocumento** | **fechaNacimiento** | **correoEstudiante** |
| Juan Pérez García | Cédula | 1001001001 | 21/11/2001 | juan.perez@galactica.edu |
| María López Rodríguez | Cédula | 1001001002 | 15/03/2002 | maria.lopez@galactica.edu |
| Carlos Gómez Martínez | Cédula | 1001001003 | 10/07/2000 | carlos.gomez@galactica.edu |
| Ana García López | Cédula | 1001001004 | 08/05/2003 | ana.garcia@galactica.edu |
| Luis Fernando Rodríguez | Cédula | 1001001005 | 12/09/2001 | luis.rodriguez@galactica.edu |

---

## ⚙️ POST-PEGADO: CONFIGURAR FORMATO DE FECHA

**IMPORTANTE:** Excel a veces importa fechas como texto. Debes arreglar esto:

1. **Selecciona columna D** (fechaNacimiento)
   - Click en la letra "D" en la parte superior

2. **Click derecho** → Formato de celdas

3. **Categoría:** Fecha

4. **Formato:** `DD/MM/YYYY`

5. **OK**

Ahora las fechas estarán correctas.

---

## ✅ VERIFICACIÓN ANTES DE SUBIR

- [ ] ¿Las 5 columnas están presentes (A-E)?
- [ ] ¿Fila 1 tiene: `nombreCompleto`, `tipoDocumento`, `numeroDocumento`, `fechaNacimiento`, `correoEstudiante`?
- [ ] ¿Fila 2+ tienen datos?
- [ ] ¿Columna D está formateada como DD/MM/YYYY?
- [ ] ¿El archivo es .xlsx?

---

## ➕ AGREGAR MÁS ESTUDIANTES

Para agregar más filas:

```
Fila 6:
Fernando Peña López	Cédula	1001001006	05/12/2002	fernando.pena@galactica.edu

Fila 7:
Patricia Moreno García	Cédula	1001001007	18/01/2001	patricia.moreno@galactica.edu

Fila 8:
Roberto Sánchez Díaz	Cédula	1001001008	22/06/2003	roberto.sanchez@galactica.edu
```

**Solo copia la estructura y cambia los datos.**

---

## 🚀 SIGUIENTES PASOS

1. ✅ Copia la tabla
2. ✅ Pégala en Excel
3. ✅ Guarda como .xlsx
4. ✅ Sube a la aplicación
5. ✅ Verás: `Exitosos: 5, Errores: 0`

---

## 🆘 TROUBLESHOOTING

### ¿Se pegó todo en UNA COLUMNA?

- Usa Tab en lugar de espacios
- Copiar desde aquí: [Tabla con Tabs](README.md)

### ¿Las fechas se ven como números?

- Selecciona columna D
- Formato → Fecha → DD/MM/YYYY

### ¿Dice "Error: Correo duplicado"?

- Cambia los emails (cada uno debe ser único)

### ¿Dice "Error: Documento duplicado"?

- Cambia los números de documento (cada uno debe ser único)

---

## 📌 RESUMEN

| Paso | Acción |
|------|--------|
| 1 | Copia la tabla desde arriba |
| 2 | Excel → A1 → Pega (Ctrl+V) |
| 3 | Columna D → Formato DD/MM/YYYY |
| 4 | Guarda como .xlsx |
| 5 | Importa en la aplicación |
| 6 | ¡Listo! |

**Tiempo estimado: 5 minutos**
