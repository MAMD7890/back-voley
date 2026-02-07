#!/bin/bash

# ============================================
# Script para configurar Nginx en AWS EC2
# Uso: ./configure-nginx.sh 3.85.111.48 ~/galacticos-key.pem
# ============================================

EC2_IP=${1:-3.85.111.48}
KEY_FILE=${2:-~/galacticos-key.pem}
EC2_USER=${3:-ec2-user}

echo "Configurando Nginx en $EC2_IP..."

ssh -i "$KEY_FILE" "$EC2_USER@$EC2_IP" << 'NGINX_SCRIPT'
    set -e
    
    echo "Instalando Nginx..."
    sudo yum install -y nginx
    
    echo "Creando configuración de Nginx..."
    sudo tee /etc/nginx/conf.d/galacticos.conf > /dev/null << 'EOF'
upstream galacticos {
    server 127.0.0.1:8080;
    keepalive 32;
}

# Redirigir HTTP a HTTPS (comentar si no tienes SSL)
# server {
#     listen 80;
#     server_name _;
#     return 301 https://$host$request_uri;
# }

server {
    listen 80;
    server_name 3-85-111-48.nip.io;
    client_max_body_size 50M;

    # Logging
    access_log /var/log/nginx/galacticos_access.log;
    error_log /var/log/nginx/galacticos_error.log;

    # Proxy hacia la aplicación
    location / {
        proxy_pass http://galacticos;
        proxy_http_version 1.1;
        
        # Headers HTTP
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $server_name;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # WebSocket support
        proxy_set_header Connection "upgrade";
        proxy_set_header Upgrade $http_upgrade;
        
        # Buffering
        proxy_buffering off;
        proxy_request_buffering off;
    }

    # CORS preflight
    if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Allow-Origin' '*' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, X-Requested-With' always;
        add_header 'Access-Control-Max-Age' '7200' always;
        return 204;
    }

    # CORS headers
    add_header 'Access-Control-Allow-Origin' '*' always;
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, PATCH, OPTIONS' always;
    add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, X-Requested-With' always;
    add_header 'Access-Control-Expose-Headers' 'Authorization' always;

    # Servir archivos estáticos desde uploads
    location /uploads/ {
        alias /opt/galacticos/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss application/atom+xml image/svg+xml;
    gzip_vary on;
    gzip_comp_level 6;
}
EOF
    
    echo "Validando configuración de Nginx..."
    sudo nginx -t
    
    echo "Habilitando e iniciando Nginx..."
    sudo systemctl enable nginx
    sudo systemctl restart nginx
    
    echo "✓ Nginx configurado exitosamente"
    echo ""
    echo "Estado de Nginx:"
    sudo systemctl status nginx
    
NGINX_SCRIPT

echo "✓ Configuración de Nginx completada"
echo ""
echo "Próximos pasos:"
echo "1. Instalar SSL (opcional pero recomendado):"
echo "   sudo yum install -y certbot python3-certbot-nginx"
echo "   sudo certbot --nginx -d 3-85-111-48.nip.io"
echo ""
echo "2. Verificar que está funcionando:"
echo "   curl -v http://3-85-111-48.nip.io/api/auth/login"
