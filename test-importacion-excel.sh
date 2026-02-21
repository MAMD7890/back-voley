#!/bin/bash
# Script de prueba para la importación de estudiantes desde Excel

# Variables
API_URL="http://localhost:8080/api/estudiantes"
SEDE_ID=1
FILE_PATH="estudiantes_importar.xlsx"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== PRUEBA DE IMPORTACIÓN DE ESTUDIANTES DESDE EXCEL ===${NC}"
echo ""

# Verificar que el archivo existe
if [ ! -f "$FILE_PATH" ]; then
    echo -e "${RED}Error: Archivo $FILE_PATH no encontrado${NC}"
    exit 1
fi

echo -e "${YELLOW}Iniciando importación...${NC}"
echo "API URL: $API_URL/importar-excel"
echo "Sede ID: $SEDE_ID"
echo "Archivo: $FILE_PATH"
echo ""

# Ejecutar la petición
RESPONSE=$(curl -s -X POST \
    "$API_URL/importar-excel?sedeId=$SEDE_ID" \
    -F "file=@$FILE_PATH" \
    -w "\n%{http_code}")

# Separar respuesta y código HTTP
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

# Mostrar resultados
echo -e "${YELLOW}Código HTTP: $HTTP_CODE${NC}"
echo ""

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}Importación completada exitosamente${NC}"
    echo ""
    echo "Resultado:"
    echo "$BODY" | jq '.'
    
    # Extraer estadísticas
    EXITOSOS=$(echo "$BODY" | jq '.exitosos')
    ERRORES=$(echo "$BODY" | jq '.errores')
    TOTAL=$(echo "$BODY" | jq '.total')
    
    echo ""
    echo -e "${GREEN}✓ Exitosos: $EXITOSOS${NC}"
    echo -e "${RED}✗ Errores: $ERRORES${NC}"
    echo "Total: $TOTAL"
    
else
    echo -e "${RED}Error en la importación (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo "Respuesta:"
    echo "$BODY" | jq '.'
fi

echo ""
echo -e "${YELLOW}=== FIN DE LA PRUEBA ===${NC}"
