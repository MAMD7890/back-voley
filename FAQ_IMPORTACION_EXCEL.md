# FAQ - Importación de Estudiantes desde Excel

## Preguntas Frecuentes

### 1. ¿Cuál es el formato correcto del archivo Excel?

**Respuesta:** El archivo debe ser en formato **.xlsx** (Excel 2007+). Debe tener:
- **Primera fila**: Encabezados (títulos de columnas)
- **Filas siguientes**: Datos de estudiantes
- **Columnas**: Exactamente en el orden especificado en la documentación

### 2. ¿Qué campos son obligatorios?

**Respuesta:** Los campos requeridos son:
1. Nombres y Apellidos (Nombre Completo)
2. Tipo de Documento (TI, CC, RC, PASAPORTE)
3. Número de Documento
4. Fecha de Nacimiento
5. Correo Electrónico del Estudiante

Si alguno de estos campos está vacío, la importación fallará para ese registro.

### 3. ¿Se crea un usuario automáticamente para cada estudiante?

**Respuesta:** Sí, automáticamente se crea un usuario con:
- **Email**: El correo del estudiante
- **Contraseña**: El número de documento del estudiante
- **Rol**: STUDENT (estudiante)

Ejemplo:
```
Email: juan@example.com
Contraseña: 1234567890 (número de documento)
```

### 4. ¿Qué sucede si el email ya existe?

**Respuesta:** La importación falla para ese estudiante específico y se registra un error. Se muestra en el reporte:
```json
{
  "fila": 5,
  "nombre": "Carlos González",
  "estado": "ERROR",
  "mensaje": "El correo ya está registrado en el sistema"
}
```

Los demás estudiantes se procesan normalmente (transacción parcial).

### 5. ¿Qué sucede si el documento ya existe?

**Respuesta:** Similar al email, si el número de documento ya existe en la base de datos, se genera un error para ese registro y se continúa con los siguientes.

### 6. ¿Se crea una membresía automáticamente?

**Respuesta:** Sí, se crea una membresía con:
- **Estado**: PENDIENTE (false)
- **Fecha de Inicio**: HOY
- **Fecha de Fin**: HOY + 1 mes
- **Valor Mensual**: 50.000 COP (valor por defecto)
- **Equipo**: Sin asignar (null)

### 7. ¿Qué estado de pago tiene el estudiante importado?

**Respuesta:** El estado de pago inicial es **PENDIENTE** para todos los estudiantes importados.

### 8. ¿Puedo actualizar estudiantes existentes con esta función?

**Respuesta:** No, actualmente solo crea nuevos estudiantes. Si intenta importar un estudiante con documento duplicado, recibirá un error.

Para actualizar, use el endpoint de actualización individual:
```
PUT /api/estudiantes/{id}
```

### 9. ¿Hay límite de estudiantes que puedo importar?

**Respuesta:** Técnicamente no hay límite, pero se recomienda:
- Archivos de hasta **5000 estudiantes** por vez
- Para archivos más grandes, dividir en lotes
- Considerar el tiempo de procesamiento (aproximadamente 0.1-0.2 seg por estudiante)

### 10. ¿Qué pasa si ocurre un error durante la importación?

**Respuesta:** El sistema:
1. **No** afecta a estudiantes ya procesados
2. **Registra** el error para ese estudiante específico
3. **Continúa** procesando los siguientes
4. **Retorna** un reporte completo con aciertos y fallos

Las transacciones son atómicas por estudiante.

### 11. ¿Cómo sabe el sistema qué estudiantes fueron creados?

**Respuesta:** En la respuesta del endpoint, cada resultado tiene:
- **estado**: "EXITOSO" o "ERROR"
- **idEstudiante**: ID del estudiante creado (si fue exitoso)
- **email**: Email asignado (si fue exitoso)
- **password**: Contraseña temporal (si fue exitoso)

### 12. ¿Cuál es la contraseña inicial del estudiante?

**Respuesta:** La contraseña inicial es el **número de documento del estudiante**.

**Recomendación**: Forzar cambio de contraseña en el primer login por seguridad.

### 13. ¿Se pueden importar valores vacíos?

**Respuesta:** Sí, para campos no requeridos:
- Si no tiene valor, se guarda como NULL
- No causa error en la importación
- Solo los 5 campos requeridos son obligatorios

### 14. ¿Cómo especifico la sede?

**Respuesta:** La sede se especifica como parámetro de query en el endpoint:
```
POST /api/estudiantes/importar-excel?sedeId=1
```

Donde `1` es el ID de la sede. Todos los estudiantes importados se asignarán a esa sede.

### 15. ¿Qué pasa si la sede no existe?

**Respuesta:** Se retorna un error antes de procesar cualquier estudiante:
```json
{
  "error": "La sede especificada no existe",
  "exitosos": 0,
  "errores": 27
}
```

### 16. ¿Los valores "Sí/No" se convierten correctamente?

**Respuesta:** Sí, el sistema reconoce múltiples formatos:
- "sí" o "Sí" o "SÍ" → true
- "si" o "Si" → true
- "true" o "True" → true
- "1" → true
- Cualquier otro valor → false

### 17. ¿Cómo se manejan las fechas?

**Respuesta:** 
- Las fechas se detectan automáticamente del formato Excel
- Se aceptan fechas en cualquier formato estándar
- Si no puede parsear la fecha, se guarda como NULL
- Formato recomendado: DD/MM/YYYY

### 18. ¿Puedo importar archivos desde la URL directamente?

**Respuesta:** No, debe ser multipart form-data. Ejemplo cURL:
```bash
curl -X POST http://localhost:8080/api/estudiantes/importar-excel?sedeId=1 \
  -F "file=@estudiantes.xlsx"
```

### 19. ¿Qué información voy a recibir en la respuesta?

**Respuesta:** Recibirá:
```json
{
  "exitosos": 25,      // Estudiantes creados exitosamente
  "errores": 2,        // Estudiantes que fallaron
  "total": 27,         // Total procesado
  "resultados": [...]  // Array detallado de cada resultado
}
```

### 20. ¿Se envían emails a los estudiantes con sus credenciales?

**Respuesta:** No, el sistema solo importa los datos. Las credenciales se retornan en la respuesta.

**Próxima mejora sugerida**: Envío automático de credenciales por email.

### 21. ¿Qué campos son enumeraciones?

**Respuesta:** Los siguientes campos tienen valores predefinidos:

**Tipo de Documento:**
- TI (Tarjeta de Identidad)
- CC (Cédula de Ciudadanía)
- RC (Registro Civil)
- PASAPORTE

**Sexo:**
- MASCULINO
- FEMENINO
- OTRO

**Jornada:**
- MAÑANA
- TARDE
- NOCHE
- UNICA

**Dominancia:**
- DERECHA
- IZQUIERDA
- AMBIDIESTRO

**Nivel Actual:**
- INICIANTE
- INTERMEDIO
- AVANZADO

Si especifica valores diferentes, se intentará una conversión automática. Si falla, se deja en NULL.

### 22. ¿Se valida el formato de email?

**Respuesta:** El sistema:
1. Acepta cualquier string en el campo de email
2. **No** valida formatos (ese es trabajo del frontend)
3. Se verifica que sea único en la base de datos

Recomendación: Validar formato en el Excel antes de importar.

### 23. ¿Se pueden importar menores de edad?

**Respuesta:** Sí, se acepta cualquier fecha de nacimiento. El sistema calcula automáticamente la edad si está vacía.

### 24. ¿Hay un límite de caracteres por campo?

**Respuesta:** Sí, según la configuración de la base de datos:
- Nombres cortos: 50 caracteres
- Nombres largos: 200 caracteres
- Direcciones: 200 caracteres
- Campos de texto: Depende del tipo

Si excede, la base de datos rechaza y genera error.

### 25. ¿Se hace backup antes de importar?

**Respuesta:** No, el sistema **no** hace backup automático. 

**Recomendación importante**: 
1. Hacer backup de la BD antes de importaciones masivas
2. Probar primero con un archivo pequeño
3. Verificar datos después de importar

### 26. ¿Cómo puedo monitorear la importación?

**Respuesta:** 
1. Revisar el reporte retornado por el endpoint
2. Verificar logs del servidor (console.log y System.out.println)
3. Consultar la tabla `estudiante` directamente
4. Verificar tabla `usuario` para confirmar credenciales

### 27. ¿Se pueden importar archivo muy grandes?

**Respuesta:** Sí, pero:
- A mayor archivo, mayor tiempo de procesamiento
- Mayor uso de memoria
- Se recomienda dividir en lotes si > 5000 registros
- Configurar timeout apropiado en el cliente

### 28. ¿Cómo reporto un error encontrado durante la importación?

**Respuesta:** Incluya en el reporte:
1. El archivo Excel (o muestra)
2. El ID del estudiante (fila)
3. El nombre del estudiante
4. El mensaje de error exacto
5. Los logs del servidor

### 29. ¿Se puede automatizar esta importación?

**Respuesta:** Sí, el endpoint está disponible para:
- Scripts automatizados
- Integraciones con otros sistemas
- Cronjobs
- Pipelines CI/CD

### 30. ¿Dónde puedo encontrar ejemplos de uso?

**Respuesta:** Consulte los archivos:
- `IMPORTACION_MASIVA_ESTUDIANTES_EXCEL.md` - Documentación completa
- `Galacticos_Importacion_Excel_Postman.json` - Colección Postman
- `test-importacion-excel.sh` - Script de prueba bash
- `EJEMPLO_RESPUESTA_IMPORTACION_EXCEL.json` - Respuesta ejemplo

---

## Soporte

Para más información o reportar problemas, consulte:
- Documentación: `IMPORTACION_MASIVA_ESTUDIANTES_EXCEL.md`
- Equipo de desarrollo: [contacto]
- Issues: GitHub Repository

---

**Última actualización**: 16/02/2026
