# 🔍 GUÍA DE DEPURACIÓN - IMPORTACIÓN EXCEL

## Cuando algo NO FUNCIONA...

### 1️⃣ VERIFICAR QUE EL BACKEND ESTÁ CORRIENDO

```bash
# En terminal del backend
mvn spring-boot:run

# Deberías ver:
# [...]
# c.s.g.GalacticoApplication : Started GalacticoApplication
# 2024-01-15 10:30:45.123 INFO (...) : APLICACIÓN INICIADA CORRECTAMENTE
```

**Si NO aparece eso**: El backend está caído. Reinicia.

---

### 2️⃣ VERIFICAR QUE EL ENDPOINT RESPONDE

Prueba con curl:

```bash
# Test simple (sin archivo)
curl -X POST \
  "http://localhost:8080/api/estudiantes/importar-excel?sedeId=2"

# Debería responder (aunque sea con error)
# Si responde: HTTP 404 o error → Backend está vivo ✅
# Si NO responde (timeout): Backend está caído ❌
```

---

### 3️⃣ VERIFICAR QUE EL ARCHIVO ES .XLSX VÁLIDO

```bash
# En PowerShell (Windows)
$file = "C:\ruta\archivo.xlsx"
$file | Get-Item | Select Name, Length

# Debería mostrar:
# Name                     Length
# ----                     ------
# archivo.xlsx             45678

# Si Length es 0 → Archivo vacío ❌
# Si Length > 10MB → Archivo demasiado grande ❌
```

---

### 4️⃣ ERRORES MÁS COMUNES Y SOLUCIONES

#### ❌ Error: "No se encontró el rol STUDENT"
```
Causa: La BD no tiene el rol STUDENT con ID=4
Solución:
1. Verifica que se ejecutó schema.sql al iniciar
2. En MySQL: SELECT * FROM rol WHERE nombre = 'STUDENT';
3. Si no existe, insertalo manualmente:
   INSERT INTO rol (id, nombre) VALUES (4, 'STUDENT');
```

#### ❌ Error: "Sede no encontrada"
```
Causa: El sedeId en el URL no existe
Solución:
1. Verifica que existe en BD: SELECT * FROM sede WHERE id = 2;
2. Usa un sedeId que exista
3. O crea una nueva sede en la aplicación
```

#### ❌ Error: "Invalid file format"
```
Causa: El archivo NO es .xlsx válido (quizás es .xls, .csv, .txt)
Solución:
1. Descarga nuevamente desde la plantilla
2. Abre en Excel → Guarda como → Formato .xlsx
3. Verifica que el archivo NO está abierto en otra aplicación
```

#### ❌ Error: "409 Conflict - Duplicado"
```
Causa: El email o documento ya existen en BD
Solución:
1. Cambia el email a uno único: juan.perez@gmail.com → juan.perez.2@gmail.com
2. O usa un documento diferente
3. O elimina el registro anterior en BD
```

#### ❌ Error: "Fecha de nacimiento requerida"
```
Causa: 
- Celda vacía en columna D
- Formato de fecha incorrecto (NO es DD/MM/YYYY)

Solución:
1. Asegúrate que TODAS las filas tienen fecha
2. Formato EXACTO: DD/MM/YYYY (ej: 21/11/2001)
3. NO usar: 21-11-2001, 2001-11-21, 21.11.2001
4. En Excel: Click derecho en columna D → Formato de celdas → Tipo "Fecha" → Formato "DD/MM/YYYY"
```

#### ❌ Error: "Correo electrónico requerido" 
```
Causa: Celda vacía en columna E o email mal formateado
Solución:
1. Asegúrate que TODAS las filas tienen email
2. Formato: usuario@dominio.com (ej: juan@example.com)
3. NO dejes espacios: "juan@example.com " ← MAL (hay espacio al final)
```

#### ❌ Error: "Nombre completo requerido"
```
Causa: Celda vacía en columna A
Solución:
1. Asegúrate que TODAS las filas tienen nombre
2. Mínimo 3 caracteres
3. Puede tener: letras, espacios, acentos, apóstrofos
4. NO números
```

---

### 5️⃣ VERIFICAR EN LOS LOGS DEL BACKEND

Cuando hagas la importación, el backend mostrará en consola:

```
2024-01-15 10:35:22.456 INFO (...) : Procesando importación de Excel...
2024-01-15 10:35:22.500 INFO (...) : Rol STUDENT validado: ID=4
2024-01-15 10:35:22.520 INFO (...) : 5 filas encontradas en el Excel
2024-01-15 10:35:22.530 INFO (...) : Procesando fila 2: Juan Pérez García
2024-01-15 10:35:22.540 INFO (...) : Procesando fila 3: María López Rodríguez
...
2024-01-15 10:35:22.600 INFO (...) : Importación completada: 5 exitosos, 0 errores
```

**Si ves:**
- "Rol STUDENT validado" → ✅ Rol existe
- "5 filas encontradas" → ✅ Excel se leyó correctamente
- "Importación completada" → ✅ Proceso terminó

**Si NO ves eso:**
- Hay error en backend → Revisa el stack trace completo en los logs

---

### 6️⃣ VERIFICAR EN LA BASE DE DATOS

Después de importar, verifica que se crearon los registros:

```sql
-- ¿Se crearon los estudiantes?
SELECT COUNT(*) as total_estudiantes FROM estudiante;

-- ¿Se crearon los usuarios?
SELECT COUNT(*) as total_usuarios FROM usuario;

-- ¿Los usuarios tienen el rol correcto?
SELECT u.*, r.nombre as rol FROM usuario u
LEFT JOIN rol r ON u.id_rol = r.id
WHERE u.id_rol = 4;

-- ¿Los datos se grabaron correctamente?
SELECT * FROM estudiante WHERE nombre_completo LIKE '%Juan%';
```

---

### 7️⃣ PRUEBA CON CURL (Verificación técnica)

```bash
# Crear archivo JSON de prueba primero
$json = @{
    nombreCompleto = "Juan Test"
    tipoDocumento = "Cédula"
    numeroDocumento = "9999999999"
    fechaNacimiento = "21/11/2001"
    correoEstudiante = "juantest@example.com"
} | ConvertTo-Json

# Luego con archivo Excel real:
curl -X POST `
  "http://localhost:8080/api/estudiantes/importar-excel?sedeId=2" `
  -F "file=@C:\ruta\plantilla-estudiantes.xlsx" `
  -v  # Agregar -v para ver headers detallados
```

---

### 8️⃣ CHECKLISTS PRE-IMPORTACIÓN

**Antes de enviar el archivo:**

```
✓ ¿El backend está corriendo? → mvn spring-boot:run
✓ ¿El archivo es .xlsx? → No .xls ni .csv
✓ ¿Tiene encabezados en fila 1? → nombreCompleto | tipoDocumento | ...
✓ ¿Los datos comienzan en fila 2? → No en fila 1
✓ ¿NO hay filas vacías? → Elimina espacios en blanco
✓ ¿Las fechas están en DD/MM/YYYY? → Ej: 21/11/2001
✓ ¿Todos los emails son únicos? → No duplicados
✓ ¿Todos los documentos son únicos? → No duplicados
✓ ¿El sedeId existe en BD? → Consulta en la app
✓ ¿NO hay caracteres especiales en documentos? → Solo números/letras
✓ ¿El archivo NO está abierto en Excel? → Ciérralo antes
✓ ¿El tamaño es < 10MB? → No demasiado grande
```

---

### 9️⃣ MONITOREAR LA IMPORTACIÓN EN TIEMPO REAL

**En el navegador (DevTools - F12):**

1. Abre pestana "Network"
2. Haz click en "Importar"
3. Busca el request POST a `/api/estudiantes/importar-excel`
4. Haz click sobre él
5. Ve la pestaña "Response" para ver la respuesta completa

```json
// Response exitosa:
{
  "exitosos": 5,
  "errores": 0,
  "total": 5,
  "mensaje": "Importación completada: 5 exitosos, 0 errores",
  "detalles": []
}

// Response con errores:
{
  "exitosos": 3,
  "errores": 2,
  "total": 5,
  "mensaje": "Importación completada: 3 exitosos, 2 errores",
  "detalles": [
    {
      "fila": 2,
      "errores": ["Email ya existe: juan@example.com"]
    },
    {
      "fila": 4,
      "errores": ["Fecha de nacimiento requerida", "Email requerido"]
    }
  ]
}
```

---

### 🔟 VARIABLES DE ENTORNO A VERIFICAR

En `application-prod.properties` o `application.properties`:

```properties
# ¿El puerto es correcto?
server.port=8080

# ¿La BD está conectada?
spring.datasource.url=jdbc:mysql://localhost:3306/galactica
spring.datasource.username=root
spring.datasource.password=...

# ¿El tamaño de upload está bien?
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# ¿El logging está activado?
logging.level.root=INFO
logging.level.com.sgg.galactica=DEBUG
```

---

### 1️⃣1️⃣ CONTACTAR SOPORTE CON INFORMACIÓN ÚTIL

Si nada funciona, recopila:

```
1. Pantalla completa del error
2. Primeras 50 líneas de logs del backend (Ctrl+C para copiar)
3. Información del archivo:
   - Nombre: ___________
   - Tamaño: ___________
   - Fechas en formato: ___________
4. Número de filas en el Excel: ___________
5. Salida de: curl "http://localhost:8080/api/health"
```

---

## ✨ RESUMEN

```
❌ Problema → Solución

No responde → Backend no corre
Error 404 → Endpoint no existe (revisar versión)
Error rol → Insertar rol STUDENT en BD
Fecha falla → Cambiar a DD/MM/YYYY en Excel
Email duplicado → Cambiar email a único
Archivo no sube → Verificar que es .xlsx
Fila vacía → Eliminar filas en blanco
```

