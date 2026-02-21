# 🧪 GUÍA DE TESTING - Endpoint Excel Import

**Fecha**: 16 de Febrero de 2026  
**Endpoint**: `POST /api/estudiantes/importar-excel?sedeId={id}`  
**Estado**: ✅ Ready for Testing

---

## 📋 PRE-REQUISITOS

1. ✅ Aplicación ejecutando en `http://localhost:8080`
2. ✅ JWT Token válido (si @PreAuthorize está habilitado)
3. ✅ Sede con ID=1 existe en la BD
4. ✅ Rol "ESTUDIANTE" existe en tabla `rol`
5. ✅ Archivo Excel en formato .xlsx

---

## 🧪 CASO 1: Importación Exitosa Completa

### Preparación
```
1. Crear archivo estudiantes.xlsx con estructura:
   - Fila 1: Encabezados
   - Filas 2-11: 10 estudiantes válidos
   
2. Datos válidos (ejemplo):
   Nombre: Juan Pérez García
   Tipo Doc: CC
   Número Doc: 1234567890
   Fecha Nac: 2005-03-15
   Correo: juan.perez@example.com
```

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes.xlsx"
```

### Response Esperada (HTTP 200)
```json
{
  "exitosos": 10,
  "errores": 0,
  "total": 10,
  "timestamp": "2026-02-16T23:35:00Z",
  "resultados": [
    {
      "fila": 2,
      "estudianteId": 450,
      "nombreEstudiante": "Juan Pérez García",
      "usuarioCreado": "juan.perez.450",
      "passwordGenerada": "K9m@xPzQ2L!",
      "estado": "exitoso",
      "mensaje": "Estudiante y usuario creados correctamente"
    },
    ...
  ]
}
```

### Verificaciones
- [ ] HTTP Status Code es 200
- [ ] Campo `timestamp` existe y es ISO 8601
- [ ] `exitosos` = 10
- [ ] `errores` = 0
- [ ] Cada resultado tiene `usuarioCreado` no null
- [ ] Cada resultado tiene `passwordGenerada` no null
- [ ] BD: Se crearon 10 Estudiantes
- [ ] BD: Se crearon 10 Usuarios con username y password hasheado
- [ ] BD: Se crearon 10 Membresías

---

## 🧪 CASO 2: Importación Con Errores Parciales

### Preparación
```
Archivo con 27 estudiantes:
- Filas 2-11: 10 estudiantes válidos
- Fila 12: Email duplicado
- Fila 13: Documento duplicado
- Filas 14-27: 14 estudiantes válidos
```

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes_con_errores.xlsx"
```

### Response Esperada (HTTP 200)
```json
{
  "exitosos": 25,
  "errores": 2,
  "total": 27,
  "timestamp": "2026-02-16T23:36:00Z",
  "resultados": [
    ... (10 exitosos),
    {
      "fila": 12,
      "nombreEstudiante": "María López",
      "estado": "error",
      "mensaje": "El correo ya está registrado en el sistema",
      "detalles": "correo.maria@example.com"
    },
    {
      "fila": 13,
      "nombreEstudiante": "Carlos García",
      "estado": "error",
      "mensaje": "El número de documento ya está registrado",
      "detalles": "1234567890"
    },
    ... (14 exitosos)
  ]
}
```

### Verificaciones
- [ ] HTTP Status Code es 200
- [ ] `exitosos` = 25
- [ ] `errores` = 2
- [ ] `total` = 27
- [ ] Errores tienen `detalles` con información adicional
- [ ] BD: Se crearon solo 25 Estudiantes (no 27)

---

## 🧪 CASO 3: Archivo No Seleccionado

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -H "Content-Type: multipart/form-data"
```

### Response Esperada (HTTP 400)
```json
{
  "error": "Archivo no seleccionado",
  "detalles": "El campo 'file' es requerido en el form-data"
}
```

### Verificaciones
- [ ] HTTP Status Code es 400
- [ ] Campo `error` contiene mensaje claro
- [ ] Campo `detalles` explica lo que falta

---

## 🧪 CASO 4: Formato de Archivo Inválido

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes.pdf"
```

### Response Esperada (HTTP 400)
```json
{
  "error": "Formato de archivo inválido",
  "detalles": "Solo se aceptan archivos .xlsx (Excel 2007+)"
}
```

### Verificaciones
- [ ] HTTP Status Code es 400
- [ ] Se rechaza .pdf
- [ ] Se rechaza .csv
- [ ] Se rechaza .xls (Excel antiguo)
- [ ] Solo acepta .xlsx

---

## 🧪 CASO 5: Archivo Muy Grande

### Preparación
```
Crear archivo de más de 10MB
(Por ejemplo, con 100,000+ filas)
```

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=1" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes_grande.xlsx"
```

### Response Esperada (HTTP 413)
```json
{
  "error": "Archivo demasiado grande",
  "detalles": "El archivo no debe exceder 10MB"
}
```

### Verificaciones
- [ ] HTTP Status Code es 413 (Payload Too Large)
- [ ] El archivo no se procesa
- [ ] No se crea ningún estudiante

---

## 🧪 CASO 6: Sede Inválida (sedeId = 0)

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=0" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes.xlsx"
```

### Response Esperada (HTTP 400)
```json
{
  "error": "Sede inválida",
  "detalles": "El parámetro sedeId es requerido y debe ser mayor a 0"
}
```

### Verificaciones
- [ ] HTTP Status Code es 400
- [ ] Se rechaza sedeId <= 0
- [ ] Se rechaza sedeId null

---

## 🧪 CASO 7: Sede No Existe

### Request
```bash
curl -X POST "http://localhost:8080/api/estudiantes/importar-excel?sedeId=999" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@estudiantes.xlsx"
```

### Response Esperada (HTTP 404)
```json
{
  "error": "Sede no encontrada",
  "detalles": "..."
}
```

### Verificaciones
- [ ] HTTP Status Code es 404
- [ ] No se crea ningún estudiante
- [ ] El archivo no se procesa

---

## 🧪 CASO 8: Validación de Credenciales Generadas

### Prerequisito
```
Importar 1 estudiante válido exitosamente
```

### Verificaciones en BD
```sql
-- Verificar Estudiante creado
SELECT * FROM estudiante WHERE numero_documento = '1234567890';

-- Verificar Usuario creado
SELECT * FROM usuario WHERE email = 'juan.perez@example.com';

-- Verificar Username generado
SELECT * FROM usuario WHERE username LIKE '%.%';

-- Verificaciones:
- [ ] Usuario.username = "juan.perez.{id}" (formato correcto)
- [ ] Usuario.requiere_cambio_password = true
- [ ] Usuario.password != "password" (está hasheado)
- [ ] Usuario.rol_id = ID del rol "ESTUDIANTE"
- [ ] Usuario.estudiante_id = ID del estudiante creado
- [ ] Estudiante.estado_pago = "PENDIENTE"
- [ ] Membresia.estado = false (no activa aún)
```

---

## 🧪 CASO 9: Password Generado Cumple Requisitos

### Verificación
```
El password generado debe contener:
- ✅ 12 caracteres de largo
- ✅ Al menos 1 mayúscula (A-Z)
- ✅ Al menos 1 minúscula (a-z)
- ✅ Al menos 1 número (0-9)
- ✅ Al menos 1 símbolo (!@#$%^&*_-+=)

Ejemplos válidos:
- K9m@xPzQ2L!a
- A1b$CdEf2G!h
- M7n%PqRs3T!u
```

### Test Script
```python
import re

def validate_password(pwd):
    if len(pwd) != 12:
        return False, "No tiene 12 caracteres"
    
    has_upper = bool(re.search(r'[A-Z]', pwd))
    has_lower = bool(re.search(r'[a-z]', pwd))
    has_digit = bool(re.search(r'[0-9]', pwd))
    has_symbol = bool(re.search(r'[!@#$%^&*_\-+=]', pwd))
    
    if not (has_upper and has_lower and has_digit and has_symbol):
        return False, "Falta un tipo de carácter"
    
    return True, "Válido"

# Testear passwords
test_passwords = ["K9m@xPzQ2L!", "A1b$CdEf2G!", "invalid123"]
for pwd in test_passwords:
    valid, msg = validate_password(pwd)
    print(f"{pwd}: {msg}")
```

---

## 🧪 CASO 10: Timestamp en ISO 8601

### Verificación
```json
{
  "timestamp": "2026-02-16T23:35:00Z"
}
```

### Validación
```
Formato ISO 8601:
- [ ] Contiene fecha: 2026-02-16
- [ ] Contiene hora: 23:35:00
- [ ] Termina en 'Z' (Zulu/UTC)
- [ ] Formato: YYYY-MM-DDTHH:MM:SSZ
```

---

## 📊 MATRIX DE TESTING

| Caso | Descripción | Status | Fecha | Observaciones |
|------|-------------|--------|-------|---------------|
| 1 | Importación exitosa | ⏳ | | |
| 2 | Con errores parciales | ⏳ | | |
| 3 | Archivo no seleccionado | ⏳ | | |
| 4 | Formato inválido | ⏳ | | |
| 5 | Archivo muy grande | ⏳ | | |
| 6 | Sede inválida | ⏳ | | |
| 7 | Sede no existe | ⏳ | | |
| 8 | Credenciales en BD | ⏳ | | |
| 9 | Validación password | ⏳ | | |
| 10 | Timestamp ISO 8601 | ⏳ | | |

---

## 🔍 DEBUGGING

Si algo falla, revisa:

### Logs de la aplicación
```bash
# En otra terminal
tail -f target/logs/*.log

# O en Spring Boot
2026-02-16T23:35:00.000-05:00 INFO  [...] POST /api/estudiantes/importar-excel
2026-02-16T23:35:00.500-05:00 DEBUG [...] Procesando 10 filas
2026-02-16T23:35:01.000-05:00 INFO  [...] Importación completada: 10 exitosos, 0 errores
```

### Base de datos
```sql
-- Verificar sedes
SELECT * FROM sede;

-- Verificar roles
SELECT * FROM rol;

-- Verificar estudiantes creados
SELECT COUNT(*) FROM estudiante;

-- Verificar usuarios creados
SELECT COUNT(*) FROM usuario;

-- Ver último estudiante creado
SELECT * FROM estudiante ORDER BY id_estudiante DESC LIMIT 1;

-- Ver último usuario creado
SELECT * FROM usuario ORDER BY id_usuario DESC LIMIT 1;
```

### Postman Collection
```
1. Guardar como: excel-import-tests.postman_collection.json
2. Importar en Postman
3. Ejecutar tests secuencialmente
4. Verificar respuestas
```

---

## ✅ CHECKLIST DE ACEPTACIÓN

- [ ] Todos los 10 casos de prueba pasaron
- [ ] HTTP Status codes son correctos
- [ ] Respuestas JSON son válidas
- [ ] Credenciales generadas son correctas
- [ ] BD se actualiza correctamente
- [ ] Timestamp está en formato ISO 8601
- [ ] Errores se registran correctamente
- [ ] Auditoría se registra (si está implementada)
- [ ] No hay excepciones no capturadas
- [ ] Performance es aceptable (< 5 segundos para 100 estudiantes)

---

## 📝 NOTAS

- El endpoint actualmente NO requiere JWT (cuando se agregue @PreAuthorize, ajustar tests)
- sedeId es requerido en query parameter
- El archivo debe ir en form-data con key "file"
- Las contraseñas NO se envían en texto plano en respuesta (solo en response para propósitos de test)
- En producción, las contraseñas se deben enviar por email seguro

---

**Testing Guide completada** ✅  
**Autor**: GitHub Copilot  
**Fecha**: 16 de Febrero de 2026
