#!/bin/bash

# ============================================
# Script de Despliegue Automático en AWS EC2
# Uso: chmod +x deploy.sh && ./deploy.sh
# ============================================

set -e  # Salir si hay error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
EC2_IP=${1:-3.85.111.48}
EC2_USER=${2:-ec2-user}
KEY_FILE=${3:-~/galacticos-key.pem}
JAR_FILE="target/galacticos-0.0.1-SNAPSHOT.jar"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Despliegue de Galacticos en AWS${NC}"
echo -e "${BLUE}================================${NC}"

# Verificar JARizo
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}JAR no encontrado. Compilando...${NC}"
    mvnw clean package -DskipTests
    echo -e "${GREEN}✓ JAR compilado exitosamente${NC}"
else
    echo -e "${GREEN}✓ JAR encontrado: $JAR_FILE${NC}"
fi

# Verificar archivo de clave
if [ ! -f "$KEY_FILE" ]; then
    echo -e "${RED}✗ Archivo de clave no encontrado: $KEY_FILE${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Clave encontrada: $KEY_FILE${NC}"

# Transferir JAR
echo -e "${YELLOW}Transfiriendo JAR a EC2...${NC}"
scp -i "$KEY_FILE" "$JAR_FILE" "$EC2_USER@$EC2_IP:/tmp/"
echo -e "${GREEN}✓ JAR transferido${NC}"

# Ejecutar script remoto
echo -e "${YELLOW}Configurando aplicación en EC2...${NC}"

ssh -i "$KEY_FILE" "$EC2_USER@$EC2_IP" << 'REMOTE_SCRIPT'
    set -e
    
    echo "Instalando dependencias..."
    sudo yum update -y
    sudo yum install -y java-17-amazon-corretto
    
    echo "Creando estructura de directorios..."
    sudo mkdir -p /opt/galacticos/uploads
    sudo useradd -m -s /bin/bash springapp 2>/dev/null || true
    
    echo "Moviendo JAR..."
    sudo mv /tmp/galacticos-0.0.1-SNAPSHOT.jar /opt/galacticos/
    sudo chown springapp:springapp /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
    
    echo "Creando archivo de propiedades..."
    sudo tee /opt/galacticos/application-prod.properties > /dev/null << 'EOF'
server.port=8080
server.servlet.context-path=/
spring.datasource.url=jdbc:mysql://localhost:3306/galacticos_db
spring.datasource.username=galacticos
spring.datasource.password=cambiar_en_produccion
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
jwt.secret=tu-secret-key-aqui-cambia-esto-en-produccion-min-32-caracteres
jwt.expiration=86400000
logging.level.root=INFO
logging.level.galacticos_app_back=DEBUG
file.upload-dir=/opt/galacticos/uploads
EOF
    
    echo "Creando servicio systemd..."
    sudo tee /etc/systemd/system/galacticos.service > /dev/null << 'EOF'
[Unit]
Description=Galacticos Volleyball Application
After=network.target

[Service]
Type=simple
User=springapp
Group=springapp
WorkingDirectory=/opt/galacticos

ExecStart=/usr/bin/java \
  -Xmx1024m \
  -Xms512m \
  -jar galacticos-0.0.1-SNAPSHOT.jar \
  --spring.config.location=file:/opt/galacticos/application-prod.properties \
  --server.address=0.0.0.0

Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=galacticos

[Install]
WantedBy=multi-user.target
EOF
    
    echo "Habilitando e iniciando servicio..."
    sudo systemctl daemon-reload
    sudo systemctl enable galacticos.service
    sudo systemctl restart galacticos.service
    
    echo "Esperando a que inicie..."
    sleep 5
    
    echo "Verificando estado..."
    sudo systemctl status galacticos.service
    
    echo "✓ Despliegue completado en EC2"
    
REMOTE_SCRIPT

echo -e "${GREEN}✓ Configuración remota completada${NC}"

echo ""
echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Próximos Pasos:${NC}"
echo -e "${BLUE}================================${NC}"
echo ""
echo -e "${YELLOW}1. Actualizar propiedades de base de datos:${NC}"
echo "   ssh -i $KEY_FILE $EC2_USER@$EC2_IP"
echo "   sudo nano /opt/galacticos/application-prod.properties"
echo ""
echo -e "${YELLOW}2. Configurar Nginx (opcional):${NC}"
echo "   Ejecutar: ./configure-nginx.sh $EC2_IP $KEY_FILE"
echo ""
echo -e "${YELLOW}3. Probar endpoint:${NC}"
echo "   curl -X POST http://$EC2_IP:8080/api/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"email\":\"admin@example.com\",\"password\":\"password\"}'"
echo ""
echo -e "${YELLOW}4. Ver logs en tiempo real:${NC}"
echo "   ssh -i $KEY_FILE $EC2_USER@$EC2_IP"
echo "   sudo journalctl -u galacticos.service -f"
echo ""
echo -e "${GREEN}¡Despliegue completado! 🎉${NC}"
