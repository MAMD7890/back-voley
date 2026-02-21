# 🔧 FIX: StringIndexOutOfBoundsException en PasswordGenerator

## ✅ Problema Identificado

Al importar estudiantes, el sistema generaba usernames pero fallaba con:

```
StringIndexOutOfBoundsException: begin 0, end 12, length 11
    at PasswordGenerator.generateUsername(PasswordGenerator.java:75)
```

**Causa raíz:** El método `generateUsername()` estaba usando la longitud de la cadena ANTES de remover caracteres especiales, cuando debería usar la longitud DESPUÉS.

## 🔍 Análisis del Código

### Código original (INCORRECTO):
```java
return username.toString()
    .replaceAll("[^a-z0-9.]", "")
    .substring(0, Math.min(username.length(), 30));  // ❌ username.length() es el ORIGINAL
```

**Problema:**
1. `username.length()` = 30 caracteres (original con acentos y caracteres especiales)
2. `.replaceAll("[^a-z0-9.]", "")` = algunos caracteres se remueven, quedando 11 caracteres
3. `.substring(0, Math.min(30, 30))` = `.substring(0, 30)` ❌ Intenta extraer 30 caracteres de una cadena de 11

**Resultado:** StringIndexOutOfBoundsException

## ✅ Solución Implementada

Ahora usa la longitud de la cadena DESPUÉS de limpiarla:

```java
// Remover caracteres especiales y limitar a 30 caracteres
String cleaned = username.toString()
        .replaceAll("[^a-z0-9.]", "");

// Usar la longitud de la cadena limpia, no la original
if (cleaned.length() > 30) {
    return cleaned.substring(0, 30);
}
return cleaned;
```

## 📊 Cambios Realizados

### Archivo: PasswordGenerator.java (líneas 50-80)

**Antes (INCORRECTO):**
```java
// Remover caracteres especiales
return username.toString()
    .replaceAll("[^a-z0-9.]", "")
    .substring(0, Math.min(username.length(), 30));  // Máximo 30 caracteres
```

**Después (CORRECTO):**
```java
// Remover caracteres especiales y limitar a 30 caracteres
String cleaned = username.toString()
    .replaceAll("[^a-z0-9.]", "");

// Usar la longitud de la cadena limpia, no la original
if (cleaned.length() > 30) {
    return cleaned.substring(0, 30);
}
return cleaned;
```

## 📝 Ejemplos de Generación de Usernames

Con esta corrección:

| Nombre Completo | Limpieza | Username Final | Estado |
|---|---|---|---|
| Juan Pérez López | juan.perez.1 | juan.perez.1 | ✅ OK |
| María Ángela García Rodriguez | maria.angela.2 → maria.angela.2 | maria.angela.2 | ✅ OK (acentos removidos) |
| Carlos Jóse Gómez Martínez | carlos.jose.3 → carlos.jose.3 | carlos.jose.3 | ✅ OK |

## 🚀 Próximos Pasos

1. Compilar: `.\mvnw clean package -DskipTests`
2. Ejecutar: `java -jar target/galacticos-0.0.1-SNAPSHOT.jar`
3. Reintentar importación de Excel
4. Verificar que los 3 estudiantes se importen exitosamente

## ✨ Beneficios

- ✅ Maneja nombres de cualquier longitud
- ✅ Remover acentos y caracteres especiales correctamente
- ✅ Username limitado a máximo 30 caracteres
- ✅ No genera excepciones de substring

## 📌 Archivos Modificados

- ✏️ `src/main/java/galacticos_app_back/galacticos/util/PasswordGenerator.java`
  - Método: `generateUsername()` (líneas 50-80)
