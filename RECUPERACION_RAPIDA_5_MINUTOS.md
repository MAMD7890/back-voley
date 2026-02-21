# 🔧 GUÍA DE RECUPERACIÓN: DE ERROR A ÉXITO EN 5 MINUTOS

## ❌ TU SITUACIÓN ACTUAL

```
Importación fallida:
- Exitosos: 0
- Errores: 10
- Todas las filas rechazadas
```

**Causa identificada:** El archivo Excel tiene columnas vacías.

---

## ✅ SOLUCIÓN EN 5 PASOS

### PASO 1: Obtén la plantilla correcta (1 min)

**3 opciones:**

**Opción A: Plantilla lista para copiar**
```
👉 Abre: PLANTILLA_EXCEL_COPIAR_PEGAR.md
👉 Copia la tabla completa
👉 Ve al Paso 2
```

**Opción B: Crear desde cero**
```
Encabezados (Fila 1):
nombreCompleto | tipoDocumento | numeroDocumento | fechaNacimiento | correoEstudiante

Datos (Fila 2):
Juan Pérez | Cédula | 1001001001 | 21/11/2001 | juan@example.com
```

**Opción C: Descargar plantilla**
```
👉 PLANTILLA_EXCEL_CORRECTA_LISTA_PARA_USAR.md
```

---

### PASO 2: Abre Excel (1 min)

```
1. Abre Microsoft Excel o LibreOffice Calc
2. Crea libro nuevo (Ctrl+N)
3. Haz clic en celda A1
```

---

### PASO 3: Pega la plantilla (1 min)

```
1. Ctrl+V (Pegar)
2. Excel distribuye automáticamente en columnas
3. Verifica que se vea así:

   Columna A: nombreCompleto
   Columna B: tipoDocumento
   Columna C: numeroDocumento
   Columna D: fechaNacimiento
   Columna E: correoEstudiante
```

---

### PASO 4: Arregla las fechas (1 min)

```
1. Click en la letra "D" (columna completa)
2. Click derecho → Formato de celdas
3. Tipo: Fecha
4. Formato: DD/MM/YYYY
5. OK
```

---

### PASO 5: Sube el archivo (1 min)

```
1. Guarda: Ctrl+S → Formato .xlsx
2. Ve a la aplicación web
3. Sube el archivo
4. Espera...
5. ✅ Verás: "Exitosos: 5, Errores: 0"
```

---

## 🎯 RESULTADO ESPERADO

### Antes (Ahora):
```
Importación de: plantilla-estudiantes-2026-02-20 (6).xlsx
Exitosos: 0 ❌
Errores: 10 ❌
```

### Después (Con plantilla correcta):
```
Importación de: plantilla-estudiantes-correcta.xlsx
Exitosos: 5 ✅
Errores: 0 ✅
```

---

## 📋 VERIFICACIÓN RÁPIDA

Antes de subir, verifica:

```
☐ Fila 1: ¿Encabezados correctos?
  A1: nombreCompleto
  B1: tipoDocumento
  C1: numeroDocumento
  D1: fechaNacimiento
  E1: correoEstudiante

☐ Fila 2: ¿Datos presentes?
  A2: Nombre (no vacío)
  B2: Cédula (no vacío)
  C2: Número (no vacío)
  D2: Fecha DD/MM/YYYY (no vacío)
  E2: Email (no vacío)

☐ Filas 3+: ¿Más datos?
  (Opcional - puedes importar desde 1 hasta cientos)

☐ Archivo: ¿.xlsx?
  ✅ Microsoft Excel 2007+
  ❌ No .xls
  ❌ No .csv
  ❌ No .txt
```

---

## 🚀 SI FALLA DE NUEVO

**Verifica estos puntos en orden:**

### 1️⃣ Excel tiene estructura correcta
```
Abre tu Excel → Fila 1
A1 = "nombreCompleto" ← EXACTO (sin espacios extra)
B1 = "tipoDocumento"  ← EXACTO
C1 = "numeroDocumento" ← EXACTO
D1 = "fechaNacimiento" ← EXACTO
E1 = "correoEstudiante" ← EXACTO
```

### 2️⃣ Datos están en fila 2+
```
A2 = Nombre del estudiante (no vacío)
B2 = "Cédula" o similar (no vacío)
C2 = Número de ID (no vacío)
D2 = Fecha formato DD/MM/YYYY (no vacío)
E2 = Email válido (no vacío)
```

### 3️⃣ Columnas están separadas
```
❌ TODO en una columna: A1="nombreCompleto tipoDocumento..."
✅ Separadas: A1="nombreCompleto", B1="tipoDocumento"

Si todo está en una columna:
→ Copia/Pega como "Pegar especial" → "Datos delimitados por tabulaciones"
```

### 4️⃣ Fechas en formato DD/MM/YYYY
```
✅ Correcto: 21/11/2001
❌ Incorrecto: 2001-11-21
❌ Incorrecto: 21-11-2001
❌ Incorrecto: 11/21/2001
```

### 5️⃣ Emails válidos y únicos
```
✅ juan@example.com (cada uno diferente)
✅ maria@example.com
❌ juan (sin @)
❌ juan@.com
❌ Todos iguales
```

---

## 📞 PASOS DE DEPURACIÓN

Si ves errores específicos, consulta:

| Error | Solución |
|-------|----------|
| "Nombre completo requerido" | Llena columna A |
| "Tipo de documento requerido" | Llena columna B |
| "Número de documento requerido" | Llena columna C |
| "Fecha de nacimiento requerida" | Llena columna D en DD/MM/YYYY |
| "Correo electrónico requerido" | Llena columna E |
| "Email ya existe" | Usa emails diferentes en cada fila |
| "Documento duplicado" | Usa documentos diferentes |

---

## ✨ DESPUÉS DE IMPORTAR EXITOSAMENTE

Cuando veas `Exitosos: 5, Errores: 0`:

```
✅ Los estudiantes se registraron automáticamente
✅ Se crearon sus usuarios
✅ Se les asignó el rol STUDENT
✅ Se generaron contraseñas aleatorias
✅ Pueden hacer login inmediatamente

📧 Cada estudiante recibe:
- Email: (el que pusiste en E)
- Contraseña: (generada aleatoriamente)
- Rol: STUDENT
- Estado: Activo

⚠️ Deben cambiar la contraseña en el primer login
```

---

## 🎯 PRÓXIMOS PASOS

1. **Ahora:** Crea el Excel correcto (5 min)
2. **Luego:** Importa (< 1 min)
3. **Después:** Distribuye las credenciales a los estudiantes
4. **Final:** Los estudiantes pueden usar la plataforma

---

## 💡 TIPS PROFESIONALES

### Importar muchos estudiantes
```
1. Prepara un Excel con 100+ filas
2. Asegúrate que formatos sean correctos
3. Sube de una vez
4. Se procesan todos en segundos
```

### Actualizar información existente
```
⚠️ No se puede actualizar con importación
Solución: Usa el panel de administración
```

### Cambiar contraseña de estudiante
```
Estudiante login → Perfil → Cambiar contraseña
```

### Exportar lista de estudiantes
```
(Función disponible en dashboard admin)
```

---

## 🎉 CHECKLIST FINAL

- [ ] Descargué PLANTILLA_EXCEL_COPIAR_PEGAR.md
- [ ] Copié la tabla
- [ ] Abrí Excel
- [ ] Pegué en A1
- [ ] Formateé columna D como DD/MM/YYYY
- [ ] Guardé como .xlsx
- [ ] Subí el archivo
- [ ] Veo "Exitosos: 5"

**¡Cuando completes esto, tu importación funcionará!**

---

## 📧 RESUMEN

```
❌ ERROR: Estructura de Excel incorrecta
✅ SOLUCIÓN: Usa plantilla provided
✅ RESULTADO: Importación exitosa
✅ TIEMPO: 5 minutos
```

**¡Vamos! Inténtalo ahora mismo.**
