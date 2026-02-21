# GUÍA RÁPIDA - Importar Estudiantes desde Excel

## ⚡ En 3 Pasos

### Paso 1️⃣ - Preparar el Excel

**Descargar plantilla y llenarla con:**
- Nombres y Apellidos
- Tipo de Documento (CC, TI, RC, PASAPORTE)
- Número de Documento
- Fecha de Nacimiento (DD/MM/YYYY)
- Todos los demás campos requeridos...

**Guardar como**: `estudiantes.xlsx` (Excel 2007+)

### Paso 2️⃣ - Acceder a la Plataforma

```
URL: http://localhost:8080/api/estudiantes/importar-excel
```

O usar **Postman**:
1. Descargar: `Galacticos_Importacion_Excel_Postman.json`
2. Importar en Postman
3. Seleccionar archivo
4. Enviar

### Paso 3️⃣ - Validar Resultado

Recibirá respuesta con:
- ✅ Cantidad de estudiantes creados
- ❌ Cantidad de errores (si hay)
- 📋 Detalles de cada uno
- 🔑 Email y contraseña temporal

---

## 📋 Estructura Mínima del Excel

| Columna | Ejemplo | Obligatorio |
|---------|---------|------------|
| Nombres y Apellidos | Juan Pérez | ✅ SÍ |
| Tipo de Documento | CC | ✅ SÍ |
| Número de Documento | 1234567890 | ✅ SÍ |
| Fecha de Nacimiento | 15/05/2010 | ✅ SÍ |
| Correo Electrónico | juan@example.com | ✅ SÍ |
| ... | ... | ❌ NO |

---

## 🔐 Credenciales Generadas

Cada estudiante obtiene automáticamente:

```
Email: [su_correo_del_excel]
Contraseña: [su_documento_del_excel]
```

**Ejemplo:**
```
Email: maria@example.com
Contraseña: 1234567890
```

---

## ✨ Lo Que Sucede Automáticamente

Para cada estudiante importado:
1. ✅ Se crea usuario en el sistema
2. ✅ Se genera membresía PENDIENTE
3. ✅ Se asigna estado de pago PENDIENTE
4. ✅ Se activa el usuario para login
5. ✅ Se guardan todos los datos personales

---

## ⚠️ Posibles Errores y Soluciones

### Error: "El archivo debe ser .xlsx"
→ Guardar Excel como **2007+** (.xlsx), no antiguo

### Error: "El correo ya está registrado"
→ Ese email ya existe en el sistema, usar otro

### Error: "Número de documento ya existe"
→ Ese documento ya está registrado, verificar

### Error: "La sede especificada no existe"
→ Crear la sede primero en administración

### Algunos estudiantes funcionan, otros no
→ NORMAL: Los que fallaron se reportan, los exitosos se crean

---

## 📞 Soporte Rápido

**¿Cuánto demora?**
- 5-10 segundos por 100 estudiantes

**¿Se pueden actualizar estudiantes?**
- No, solo crear nuevos

**¿Se envían emails con credenciales?**
- No automáticamente, se retornan en la respuesta

**¿Se pierden datos si falla?**
- No, los exitosos se guardan de todas formas

---

## 🎯 Flujo Típico

```
1. Lleno formulario en Excel
   ↓
2. Guardo como .xlsx
   ↓
3. Subo a http://localhost:8080/api/estudiantes/importar-excel?sedeId=1
   ↓
4. Recibo respuesta con:
   - Cantidad exitosa
   - Cantidad errores
   - Detalles
   ↓
5. Los estudiantes pueden hacer login con:
   - Email: correo del Excel
   - Contraseña: documento del Excel
```

---

**¡Listo! Ya puede importar estudiantes masivamente 🎉**
