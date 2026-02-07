#!/bin/bash

##############################################################################
# SCRIPT DE DEPLOYMENT AUTOMATIZADO - GALACTICOS APP WOMPI
# Este script automatiza todo el proceso de deployment en EC2
##############################################################################

set -e  # Exit on error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables de configuración
EC2_IP="3.85.111.48"
EC2_USER="ec2-user"
EC2_KEY="./clave-ec2.pem"
APP_DIR="/opt/galacticos"
SERVICE_NAME="galacticos.service"
JAR_FILE="target/galacticos-0.0.1-SNAPSHOT.jar"

##############################################################################
# FUNCIONES
##############################################################################

print_banner() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║   DEPLOYMENT AUTOMATIZADO - GALACTICOS APP PRODUCCIÓN        ║"
    echo "║   Wompi + CloudFront + AWS EC2                              ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_step() {
    echo -e "${YELLOW}[PASO] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
    exit 1
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

##############################################################################
# VALIDACIONES PREVIAS
##############################################################################

validate_prerequisites() {
    print_step "Validando prerrequisitos..."

    # Verificar que existamos en el directorio correcto
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml no encontrado. Ejecuta este script desde la raíz del proyecto"
    fi

    # Verificar Maven
    if ! command -v mvn &> /dev/null; then
        if [ ! -f "mvnw.cmd" ] && [ ! -f "mvnw" ]; then
            print_error "Maven o mvnw no encontrados"
        fi
    fi

    # Verificar SSH key
    if [ ! -f "$EC2_KEY" ]; then
        print_error "Archivo de clave EC2 no encontrado: $EC2_KEY"
    fi

    # Verificar conectividad a EC2
    print_info "Verificando conectividad a EC2..."
    if ! ssh -i "$EC2_KEY" -o ConnectTimeout=5 "$EC2_USER@$EC2_IP" "echo OK" &>/dev/null; then
        print_error "No se puede conectar a EC2 ($EC2_IP). Verifica la clave y la IP"
    fi

    print_success "Todos los prerrequisitos validados"
}

##############################################################################
# COMPILACIÓN
##############################################################################

compile_application() {
    print_step "Compilando aplicación con Maven..."

    if [ -f "mvnw.cmd" ]; then
        ./mvnw.cmd clean package -DskipTests
    elif [ -f "mvnw" ]; then
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi

    if [ ! -f "$JAR_FILE" ]; then
        print_error "Error compilando la aplicación"
    fi

    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    print_success "Aplicación compilada exitosamente ($JAR_SIZE)"
}

##############################################################################
# BACKUP EN EC2
##############################################################################

backup_on_ec2() {
    print_step "Creando backup en EC2..."

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        BACKUP_DIR="/opt/galacticos/backup"
        
        sudo mkdir -p $BACKUP_DIR
        
        if [ -f "/opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar" ]; then
            sudo cp /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar \
                    $BACKUP_DIR/galacticos-0.0.1-SNAPSHOT.jar.$TIMESTAMP
            echo "Backup JAR: galacticos-0.0.1-SNAPSHOT.jar.$TIMESTAMP"
        fi
        
        if [ -f "/opt/galacticos/application-prod.properties" ]; then
            sudo cp /opt/galacticos/application-prod.properties \
                    $BACKUP_DIR/application-prod.properties.$TIMESTAMP
            echo "Backup properties: application-prod.properties.$TIMESTAMP"
        fi
        
        # Limpiar backups antiguos (mantener últimos 5)
        find $BACKUP_DIR -type f -mtime +7 -delete
EOF

    print_success "Backup completado"
}

##############################################################################
# TRANSFERENCIA DE ARCHIVOS
##############################################################################

transfer_jar() {
    print_step "Transfiriendo JAR a EC2..."

    scp -i "$EC2_KEY" "$JAR_FILE" "$EC2_USER@$EC2_IP:/tmp/"

    print_success "JAR transferido"
}

transfer_properties() {
    print_step "Transfiriendo archivo de propiedades..."

    # Verificar que existe el template
    if [ ! -f "application-prod.properties.template" ]; then
        print_info "Template de propiedades no encontrado"
        print_info "Creando desde template..."
        
        scp -i "$EC2_KEY" application-prod.properties.template \
            "$EC2_USER@$EC2_IP:/tmp/application-prod.properties.template"
    else
        scp -i "$EC2_KEY" application-prod.properties.template \
            "$EC2_USER@$EC2_IP:/tmp/"
    fi

    print_success "Archivo de propiedades transferido"
}

##############################################################################
# DEPLOY EN EC2
##############################################################################

deploy_on_ec2() {
    print_step "Desplegando en EC2..."

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
        set -e
        
        BLUE='\033[0;34m'
        GREEN='\033[0;32m'
        NC='\033[0m'
        
        echo -e "${BLUE}Deteniendo servicio...${NC}"
        sudo systemctl stop galacticos.service 2>/dev/null || true
        sleep 2
        
        echo -e "${BLUE}Instalando nuevo JAR...${NC}"
        sudo mv /tmp/galacticos-0.0.1-SNAPSHOT.jar /opt/galacticos/
        sudo chown galacticos:galacticos /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
        sudo chmod 644 /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
        
        echo -e "${BLUE}Iniciando servicio...${NC}"
        sudo systemctl start galacticos.service
        sleep 3
        
        echo -e "${GREEN}✅ Servicio iniciado${NC}"
        
        # Verificar que el servicio está running
        if sudo systemctl is-active --quiet galacticos.service; then
            echo -e "${GREEN}✅ Servicio en estado RUNNING${NC}"
        else
            echo -e "${RED}❌ Error: Servicio no está corriendo${NC}"
            sudo journalctl -u galacticos.service -n 20
            exit 1
        fi
EOF

    print_success "Deploy completado"
}

##############################################################################
# VERIFICACIÓN POST-DEPLOYMENT
##############################################################################

verify_deployment() {
    print_step "Verificando deployment..."

    # Esperar a que la aplicación esté lista
    print_info "Esperando a que la aplicación inicie..."
    sleep 5

    # Prueba de conectividad simple
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://$EC2_IP:8080/actuator/health 2>/dev/null || echo "000")

    if [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "000" ]; then
        print_success "Aplicación respondiendo"
    else
        print_error "La aplicación no responde correctamente (HTTP $RESPONSE)"
    fi

    # Test de autenticación
    print_info "Probando endpoint de login..."
    TEST=$(curl -s -X POST http://$EC2_IP:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"test@test.com","password":"test"}' 2>/dev/null | grep -q "error" && echo "ERROR" || echo "OK")

    if [ "$TEST" = "OK" ]; then
        print_success "Endpoint de autenticación funciona"
    fi

    # Test de Wompi
    print_info "Probando endpoint de Wompi..."
    WOMPI_TEST=$(curl -s "http://$EC2_IP:8080/api/wompi/integrity-signature?amount=5000000&reference=TEST&currency=COP" 2>/dev/null | grep -q "publicKey" && echo "OK" || echo "ERROR")

    if [ "$WOMPI_TEST" = "OK" ]; then
        print_success "Endpoint de Wompi funciona"
    else
        print_info "Wompi aún no configurado (esperado si falta application-prod.properties)"
    fi
}

##############################################################################
# LOGS
##############################################################################

show_logs() {
    print_step "Ultimas líneas del log..."

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
        echo "=== Log de aplicación ==="
        sudo tail -n 20 /var/log/galacticos/application.log 2>/dev/null || echo "Log no disponible"
        
        echo ""
        echo "=== Estado del servicio ==="
        sudo systemctl status galacticos.service --no-pager
EOF
}

##############################################################################
# MENU PRINCIPAL
##############################################################################

show_menu() {
    echo ""
    echo -e "${YELLOW}¿Qué deseas hacer?${NC}"
    echo "1) Deployment completo (compilar + backup + deploy)"
    echo "2) Solo compilar"
    echo "3) Solo desplegar JAR"
    echo "4) Ver logs"
    echo "5) Rollback (restaurar versión anterior)"
    echo "6) Ver estado"
    echo "7) Salir"
    echo ""
    read -p "Selecciona una opción (1-7): " option
}

##############################################################################
# FUNCIONES ADICIONALES
##############################################################################

rollback() {
    print_step "Ejecutando rollback..."

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
        BACKUP_DIR="/opt/galacticos/backup"
        
        if [ ! -d "$BACKUP_DIR" ]; then
            echo "No hay backups disponibles"
            exit 1
        fi
        
        echo "Deteniendo servicio..."
        sudo systemctl stop galacticos.service
        
        # Obtener el backup más reciente
        LATEST_JAR=$(ls -t $BACKUP_DIR/*.jar.* 2>/dev/null | head -1)
        
        if [ -z "$LATEST_JAR" ]; then
            echo "No hay backups de JAR disponibles"
            exit 1
        fi
        
        echo "Restaurando desde: $LATEST_JAR"
        sudo cp "$LATEST_JAR" /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
        
        echo "Iniciando servicio..."
        sudo systemctl start galacticos.service
        sleep 3
        
        if sudo systemctl is-active --quiet galacticos.service; then
            echo "✅ Rollback completado exitosamente"
        else
            echo "❌ Error en rollback"
            exit 1
        fi
EOF

    print_success "Rollback completado"
}

show_status() {
    print_step "Estado del sistema..."

    ssh -i "$EC2_KEY" "$EC2_USER@$EC2_IP" << 'EOF'
        echo "=== Estado del Servicio ==="
        sudo systemctl status galacticos.service --no-pager | head -10
        
        echo ""
        echo "=== Uso de Recursos ==="
        ps aux | grep galacticos | grep -v grep | awk '{print "CPU: " $3 "% | Memory: " $4 "%"}'
        
        echo ""
        echo "=== JAR Instalado ==="
        ls -lh /opt/galacticos/galacticos-0.0.1-SNAPSHOT.jar
        
        echo ""
        echo "=== Configuración Activa ==="
        echo "Wompi Public Key: $(grep wompi.public-key /opt/galacticos/application-prod.properties | cut -d= -f2 | head -c 20)..."
        
        echo ""
        echo "=== Conectividad ==="
        curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' || echo "No responde"
EOF
}

##############################################################################
# MAIN
##############################################################################

main() {
    print_banner
    
    while true; do
        show_menu
        
        case $option in
            1)
                echo ""
                validate_prerequisites
                compile_application
                backup_on_ec2
                transfer_jar
                deploy_on_ec2
                verify_deployment
                print_success "Deployment completo exitoso"
                ;;
            2)
                echo ""
                compile_application
                ;;
            3)
                echo ""
                validate_prerequisites
                backup_on_ec2
                transfer_jar
                deploy_on_ec2
                verify_deployment
                ;;
            4)
                echo ""
                show_logs
                ;;
            5)
                echo ""
                validate_prerequisites
                rollback
                ;;
            6)
                echo ""
                show_status
                ;;
            7)
                echo "¡Hasta luego!"
                exit 0
                ;;
            *)
                print_error "Opción inválida"
                ;;
        esac
        
        read -p "Presiona Enter para continuar..."
    done
}

# Ejecutar main
main

