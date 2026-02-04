# 📚 Explicación Detallada: Configuración AWS para Galacticos Backend

## 🎯 Índice de Conceptos

1. [Arquitectura General](#arquitectura-general)
2. [VPC y Redes](#vpc-y-redes)
3. [EC2: El Servidor](#ec2-el-servidor)
4. [RDS: Base de Datos](#rds-base-de-datos)
5. [Grupos de Seguridad](#grupos-de-seguridad)
6. [Elastic IP](#elastic-ip)
7. [IAM: Control de Acceso](#iam-control-de-acceso)
8. [Almacenamiento (S3)](#almacenamiento-s3)
9. [Monitoreo y Alertas](#monitoreo-y-alertas)
10. [Costos y Optimización](#costos-y-optimización)

---

## Arquitectura General

### ¿Cómo Funciona Galacticos en AWS?

```
┌─────────────────────────────────────────────────────────────┐
│                        INTERNET                              │
│                   Solicitudes HTTPS                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   Route 53 (DNS)               │
        │   galacticos.com → IP Elástica│
        └────────────────────┬───────────┘
                             │
                             ▼
        ┌────────────────────────────────────┐
        │  Elastic IP (IP Pública Fija)      │
        │  203.0.113.45                      │
        └────────────────────┬───────────────┘
                             │
                             ▼
        ┌────────────────────────────────────────┐
        │         EC2 Instance (Servidor)        │
        │  ┌──────────────────────────────────┐  │
        │  │  Ubuntu 24.04 LTS                │  │
        │  │  ┌────────────────────────────┐  │  │
        │  │  │  Nginx (Reverse Proxy)     │  │  │
        │  │  │  Puerto 80, 443            │  │  │
        │  │  └──────────┬─────────────────┘  │  │
        │  │             │                     │  │
        │  │  ┌──────────▼─────────────────┐  │  │
        │  │  │  Spring Boot Application   │  │  │
        │  │  │  Puerto 8080               │  │  │
        │  │  └──────────┬─────────────────┘  │  │
        │  └─────────────┼────────────────────┘  │
        │                │                       │
        │  VPC: vpc-xxxxx│ Subnet: subnet-xxxxx │
        │  Security Group: galacticos-sg        │
        └────────────────┼───────────────────────┘
                         │
              ┌──────────▼──────────┐
              │   AWS VPC Network   │
              │  (Red Privada)      │
              └──────────┬──────────┘
                         │
        ┌────────────────▼────────────────┐
        │  RDS MySQL Instance             │
        │  db.t3.micro                    │
        │  ┌──────────────────────────┐   │
        │  │  Base de Datos           │   │
        │  │  escuela_voleibol        │   │
        │  │  Puerto 3306             │   │
        │  └──────────────────────────┘   │
        │                                 │
        │  Subnet Group: rds-subnet-group │
        │  Security Group: rds-sg         │
        └─────────────────────────────────┘
```

### Flujo de una Solicitud

```
Usuario
   │
   ├─► DNS: ¿Dónde está galacticos.com?
   │
   ├─► Route 53 responde: 203.0.113.45
   │
   ├─► HTTPS a 203.0.113.45:443
   │
   ├─► Elastic IP redirige a EC2
   │
   ├─► Nginx recibe en puerto 443
   │     (SSL/TLS terminado aquí)
   │
   ├─► Nginx envía HTTP a localhost:8080
   │
   ├─► Spring Boot procesa la solicitud
   │
   ├─► Spring Boot consulta RDS
   │     (jdbc:mysql://rds-endpoint:3306/...)
   │
   ├─► RDS ejecuta query SQL
   │
   ├─► RDS retorna datos
   │
   ├─► Spring Boot construye respuesta JSON
   │
   ├─► Nginx encripta respuesta con SSL
   │
   └─► Usuario recibe respuesta HTTPS
```

---

## VPC y Redes

### ¿Qué es una VPC?

**VPC (Virtual Private Cloud)** = Tu red privada en AWS

```
VPC: 10.0.0.0/16 (Rango de IPs disponibles: 10.0.0.0 - 10.0.255.255)
│
├─ Subnet Pública: 10.0.1.0/24 (Conectada a Internet)
│  │
│  ├─ EC2 Instance: 10.0.1.100 (IP Privada)
│  └─ Elastic IP: 203.0.113.45 (IP Pública)
│
└─ Subnet Privada: 10.0.2.0/24 (No conectada directamente a Internet)
   │
   └─ RDS Instance: 10.0.2.50 (IP Privada)
```

### Subnets (Subredes)

**Subnet Pública:**
- Se conecta a Internet Gateway
- Las instancias obtienen IP pública (o Elastic IP)
- Usada para: EC2, bastion hosts, ALB

**Subnet Privada:**
- NO se conecta directamente a Internet
- Las instancias NO obtienen IP pública
- Usada para: RDS, caché, componentes internos
- Más segura

### Internet Gateway

```
┌────────────────────┐
│  Tu Subnet Pública │
│  10.0.1.0/24       │
└────────┬───────────┘
         │
         │ Ruta por defecto (0.0.0.0/0)
         │
    ┌────▼─────────────┐
    │ Internet Gateway  │
    │ Conecta a Internet│
    └──────────────────┘
         │
         ▼
    ┌─────────────────┐
    │   INTERNET      │
    └─────────────────┘
```

### Network ACL vs Security Group

| Aspecto | Network ACL | Security Group |
|---------|-----------|-----------------|
| **Nivel** | Subnet | Instancia/ENI |
| **Reglas** | Numeradas (orden importa) | Sin orden, todas se evalúan |
| **Stateful** | No (necesita inbound Y outbound) | Sí (retorno automático) |
| **Defecto** | Deny all | Allow all outbound |
| **Caso de Uso** | Control granular de subnets | Control de instancias |

---

## EC2: El Servidor

### ¿Qué es EC2?

**EC2 (Elastic Compute Cloud)** = Servidor virtual en la nube

Es como rentar un computador, pero:
- ✅ Pagos por uso
- ✅ Sin mantenimiento físico
- ✅ Escalable (puedes aumentar/disminuir recursos)
- ✅ Flexible (puedes elegir el SO)

### Tipos de Instancias

```
t3.micro    CPU compartida variable  RAM 1GB   -> Desarrollo/Pruebas
t3.small    CPU compartida variable  RAM 2GB   -> Desarrollo/Pruebas
m5.large    CPU variable dedicada    RAM 8GB   -> Aplicaciones medianas
c5.xlarge   CPU dedicada             RAM 8GB   -> Aplicaciones CPU intensivas
r5.large    Optimizada para RAM      RAM 16GB  -> Bases de datos, caché
```

**Para Galacticos:**
- Desarrollo: `t3.micro` (1 vCPU, 1 GB RAM, capa gratuita)
- Producción: `t3.small` o `m5.large` (según carga)

### Ciclo de Vida de una Instancia EC2

```
┌─────────────┐
│   PENDING   │  <- Instancia siendo lanzada (< 1 minuto)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   RUNNING   │  <- Instancia activa (se cobra)
└──────┬──────┘
       │
       ├─► STOPPING ──► STOPPED (Se pausa, no se cobra)
       │
       ├─► SHUTTING-DOWN ──► TERMINATED (Se elimina, no se cobra)
       │
       └─► REBOOTING (Reinicio)
```

**Estados:**
- **Running**: En ejecución, se cobra
- **Stopped**: Pausada, se cobra almacenamiento pero no CPU/RAM
- **Terminated**: Eliminada (irreversible, a menos que tenga EBS snapshot)

### AMI (Amazon Machine Image)

**AMI = Plantilla de instancia (sistema operativo preconfigurado)**

```
AMI ID: ami-0c55b159cbfafe1f0
├─ Sistema Operativo: Ubuntu 24.04 LTS
├─ Arquitectura: 64-bit (x86_64)
├─ Virtualization Type: hvm
└─ Root Device Type: ebs (almacenamiento elástico)
```

### Almacenamiento EBS (Elastic Block Store)

**EBS = Disco duro virtual**

```
EBS Volume
├─ Tipo: gp3 (General Purpose SSD)
│  └─ IOPS: 3000 (operaciones por segundo)
│  └─ Throughput: 125 MB/s
│
├─ Tamaño: 30 GB
│
├─ Encriptación: sí (AES-256)
│
└─ Snapshots: Backup automático cada 12 horas
```

**Diferencias EBS vs S3:**
- **EBS**: Almacenamiento de bloque (disco duro), rápido, para SO y aplicaciones
- **S3**: Almacenamiento de objetos (archivos), lento, para backups y archivos estáticos

### Monitoreo de EC2

```
CPU Utilization
│
├─ t3.micro: 20% (Bueno, debajo del límite)
├─ t3.micro: 80% (Alerta, podría saturarse)
└─ t3.micro: >100% (Throttling, muy lento)

Memory
├─ Usada: 800 MB / 1024 MB Total
├─ % Utilizado: 78%
└─ Alerta: >85% (Reiniciar aplicación o instancia más grande)

Disk
├─ Usado: 20 GB / 30 GB Total
├─ % Utilizado: 67%
└─ Crítico: >90% (Limpiar logs, eliminar archivos)
```

---

## RDS: Base de Datos

### ¿Qué es RDS?

**RDS (Relational Database Service)** = Base de datos gestionada por AWS

```
┌─────────────────────────────────┐
│  SIN RDS (Tu responsabilidad)   │
├─────────────────────────────────┤
│ ✓ Instalación MySQL             │
│ ✓ Configuración                 │
│ ✓ Actualizaciones               │
│ ✓ Backups                       │
│ ✓ Replicación                   │
│ ✓ Monitoreo                     │
│ ✓ Recuperación ante fallos       │
│ ✗ El resto lo haces tú          │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  CON RDS (AWS lo maneja)        │
├─────────────────────────────────┤
│ ✓ Instalación MySQL             │
│ ✓ Configuración                 │
│ ✓ Actualizaciones (automáticas) │
│ ✓ Backups (automáticos)         │
│ ✓ Replicación (Multi-AZ)        │
│ ✓ Monitoreo                     │
│ ✓ Recuperación ante fallos       │
│ ✓ Escalabilidad                 │
│ ✓ Tu solo haces queries         │
└─────────────────────────────────┘
```

### Clases de BD

```
db.t3.micro    1 vCPU    1 GB RAM     -> Desarrollo
db.t3.small    1 vCPU    2 GB RAM     -> Pequeñas apps
db.m5.large    2 vCPU    8 GB RAM     -> Medianas apps
db.m5.xlarge   4 vCPU    16 GB RAM    -> Apps grandes
db.r5.large    2 vCPU    16 GB RAM    -> BD grandes (optimizada RAM)
```

**Estimación para Galacticos:**
- Desarrollo: `db.t3.micro`
- Producción: `db.t3.small` o `db.m5.large`

### Multi-AZ (Disponibilidad Alta)

```
SIN Multi-AZ (Un solo servidor):
┌──────────────────┐
│  us-east-1a      │
│  ┌──────────────┐│
│  │ RDS Primaria ││
│  └──────────────┘│
└──────────────────┘
   └─ Si falla, la BD se cae

CON Multi-AZ (Dos servidores):
┌──────────────────┐        ┌──────────────────┐
│  us-east-1a      │        │  us-east-1b      │
│  ┌──────────────┐│        │  ┌──────────────┐│
│  │ RDS Primaria ││◄─────►│  │ RDS Standby  ││
│  └──────────────┘│ Sync   │  └──────────────┘│
│  (Recibe escrit) │        │  (No recibe)     │
└──────────────────┘        └──────────────────┘
   └─ Si falla primaria, AWS cambia a standby (automático)
   └─ RECOMENDADO PARA PRODUCCIÓN
```

### Backups Automáticos

```
Período de Retención: 7 días (configurable 1-35 días)

Lunes    Martes   Miércoles  Jueves   Viernes  Sábado  Domingo
  ✓        ✓        ✓         ✓        ✓        ✓       ✓
Backup  Backup   Backup    Backup   Backup   Backup  Backup
Auto    Auto     Auto      Auto     Auto     Auto    Auto

└─ Si tu BD se corrompe el viernes a las 14:00
   Puedes restaurar a cualquier punto entre hace 7 días

Transactional Logs:
└─ Se guardan por 24 horas
└─ Permite "point-in-time recovery" (recuperar a segundo exacto)
```

### Parámetros Importantes de MySQL en RDS

```
max_connections = 100
└─ Máximo de conexiones simultaneas
└─ Para db.t3.micro: ajustar a 50-100

innodb_buffer_pool_size
└─ Caché de datos en memoria
└─ Mayor = mejor performance
└─ Para db.t3.micro: 512MB

slow_query_log = 1
└─ Registra queries lentas (> 2 segundos)
└─ Útil para optimización

binlog_retention_hours = 24
└─ Cuánto tiempo guardar binary logs
└─ Necesario para backups y replicación
```

### Monitoreo de RDS

```
CPU Utilization
├─ < 20%: Bien
├─ 20-50%: Normal
├─ 50-80%: Alerta
└─ > 80%: Considerar upgrade

Database Connections
├─ Actual: 45 conexiones
├─ Máximo permitido: 100
└─ Alerta si: > 80% del máximo

Storage
├─ Usado: 15 GB / 20 GB
├─ Porcentaje: 75%
└─ Alerta: > 85% (auto-scaling de almacenamiento)

IOPS
├─ Provisioned: 1000 IOPS
├─ Used: 450 IOPS
└─ Burst: Hasta 3000 para ráfagas

Replication Lag (si tienes replicas)
├─ < 1 segundo: Bien
├─ > 5 segundos: Problema
└─ > 30 segundos: Crítico
```

---

## Grupos de Seguridad

### ¿Qué es un Security Group?

**Security Group = Firewall virtual**

```
Regla de Entrada (Inbound):
┌────────────────────────────────┐
│ Type: HTTPS (TCP 443)          │
│ Source: 0.0.0.0/0 (Cualquiera) │
│ Action: ALLOW                  │
└────────────────────────────────┘
         └─ Usuario en Internet puede conectar a puerto 443

Regla de Salida (Outbound):
┌────────────────────────────────┐
│ Type: All traffic              │
│ Destination: 0.0.0.0/0         │
│ Action: ALLOW                  │
└────────────────────────────────┘
         └─ Instancia puede conectar a cualquier lugar
```

### Security Group para EC2 (Galacticos)

```
INBOUND RULES:
┌──────────┬────────┬──────────────┬──────────────────┐
│ Type     │ Port   │ Protocol     │ Source           │
├──────────┼────────┼──────────────┼──────────────────┤
│ SSH      │ 22     │ TCP          │ 203.0.113.1/32   │ <- Tu IP
├──────────┼────────┼──────────────┼──────────────────┤
│ HTTP     │ 80     │ TCP          │ 0.0.0.0/0        │ <- Todos
├──────────┼────────┼──────────────┼──────────────────┤
│ HTTPS    │ 443    │ TCP          │ 0.0.0.0/0        │ <- Todos
├──────────┼────────┼──────────────┼──────────────────┤
│ Custom   │ 8080   │ TCP          │ 0.0.0.0/0        │ <- Desarrollo
└──────────┴────────┴──────────────┴──────────────────┘

OUTBOUND RULES:
┌──────────┬──────────────────┐
│ Type     │ Destination      │
├──────────┼──────────────────┤
│ All      │ 0.0.0.0/0        │ <- Permite salida a todo
└──────────┴──────────────────┘
```

**Explicación:**
- **SSH (22)**: Para conectar remotamente. Restringido a TU IP
- **HTTP (80)**: Para tráfico sin encriptar. Abierto a todos
- **HTTPS (443)**: Para tráfico encriptado. Abierto a todos
- **Puerto 8080**: Solo para desarrollo. DESHABILITAR EN PRODUCCIÓN

### Security Group para RDS

```
INBOUND RULES:
┌──────────┬────────┬──────────────┬──────────────────────┐
│ Type     │ Port   │ Protocol     │ Source               │
├──────────┼────────┼──────────────┼──────────────────────┤
│ MySQL    │ 3306   │ TCP          │ sg-ec2 (SG de EC2)   │
└──────────┴────────┴──────────────┴──────────────────────┘

OUTBOUND RULES:
└─ Por defecto, deny all (RDS no inicia conexiones)
```

**Explicación:**
- RDS solo acepta conexiones desde el Security Group de EC2
- Más seguro que abrir a 0.0.0.0/0
- RDS no intenta conectarse a nada (es servidor)

### Stateful Connections

```
Solicitud normal:
┌─────────────┐         ┌──────────────┐
│   Cliente   │ ────►   │  Servidor    │  Inbound: Permitido ✓
│             │◄────    │  (Puerto 80) │  Outbound: Auto ✓
└─────────────┘         └──────────────┘

Sin regla de salida explícita:
└─ AWS permite automáticamente la respuesta
└─ Esto es "Stateful"
```

---

## Elastic IP

### ¿Qué es Elastic IP?

**Elastic IP = IP pública fija que controlas**

```
SIN Elastic IP:
EC2 Instancia
└─ IP Pública: 203.0.113.45
├─ Asignada aleatoriamente
├─ Se pierde si apagas la instancia
├─ Si creas nueva instancia, tienes nueva IP
└─ PROBLEMA: Dominio queda roto

CON Elastic IP:
EC2 Instancia
└─ IP Elástica: 203.0.113.45 (TUYA)
├─ Asignada explícitamente por ti
├─ Se mantiene aunque apagues instancia
├─ Puedes reasignarla a otra instancia
├─ VENTAJA: Dominio siempre apunta a la misma IP
└─ Se cobra si no está en uso
```

### Mapeo de Elastic IP

```
┌──────────────────────────────────┐
│  Elastic IP                      │
│  203.0.113.45                    │
└─────────────┬────────────────────┘
              │
              │ ENI (Elastic Network Interface)
              │ eth0
              │
    ┌─────────▼──────────┐
    │  EC2 Instance      │
    │  i-0d12345ab6c789ef│
    │  ┌────────────────┐│
    │  │ Private IP:    ││
    │  │ 10.0.1.100     ││
    │  └────────────────┘│
    └────────────────────┘
```

**Flujo:**
1. Usuario escribe: galacticos.com
2. DNS resuelve a: 203.0.113.45 (Elastic IP)
3. AWS redirige 203.0.113.45 → 10.0.1.100 (IP privada de EC2)
4. EC2 recibe tráfico en puerto 80/443

### Costos de Elastic IP

```
Elastic IP asociada a instancia running: GRATIS
Elastic IP sin usar (sin instancia): $0.005/hora = $3.60/mes
Elastic IP si cambias de instancia mucho: $0.005/hora por cambio

RECOMENDACIÓN:
├─ Desarrollo: No usar (capa gratuita permite 1)
├─ Producción: Usar siempre (es barato y estable)
└─ Nunca dejar IPs elásticas huérfanas
```

---

## IAM: Control de Acceso

### ¿Qué es IAM?

**IAM (Identity and Access Management)** = Sistema de permisos de AWS

```
Tu Cuenta AWS
├─ Root User (Cuenta principal, acceso completo)
│  └─ NUNCA usar para aplicaciones
│  └─ Solo para tareas administrativas
│
├─ IAM User (Usuario con permisos limitados)
│  ├─ galacticos-deployment
│  │  ├─ Permiso: ec2:DescribeInstances
│  │  ├─ Permiso: rds:DescribeDBInstances
│  │  ├─ Permiso: s3:GetObject
│  │  └─ Permiso: cloudwatch:PutMetricAlarm
│  │
│  └─ galacticos-app
│     ├─ Permiso: s3:GetObject (solo uploads/)
│     ├─ Permiso: rds-db:connect
│     └─ NO permiso: ec2:TerminateInstances
│
└─ IAM Role (Rol para servicios)
   ├─ EC2-RDS-Access
   │  ├─ Permiso: rds-db:connect
   │  ├─ Permiso: s3:GetObject
   │  └─ Permiso: kms:Decrypt
   │
   └─ Lambda-Processor
      ├─ Permiso: s3:GetObject
      ├─ Permiso: sqs:ReceiveMessage
      └─ Permiso: dynamodb:PutItem
```

### Política IAM para Galacticos

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DescribeEC2Instances",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSubnets"
      ],
      "Resource": "*"
    },
    {
      "Sid": "AccessRDS",
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:ListTagsForResource"
      ],
      "Resource": "arn:aws:rds:us-east-1:123456789:db/galacticos-db"
    },
    {
      "Sid": "AccessS3Uploads",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::galacticos-uploads/*"
    },
    {
      "Sid": "PublishCloudWatch",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData",
        "cloudwatch:GetMetricStatistics"
      ],
      "Resource": "*"
    }
  ]
}
```

### IAM Role para EC2

```
Tu Aplicación Spring Boot necesita acceso a:
├─ RDS (para conectarse a BD)
├─ S3 (para guardar uploads)
└─ CloudWatch (para enviar logs)

Solución:
1. Crear IAM Role: ec2-galacticos-role
2. Adjuntar Políticas al Role
3. Asignar Role a Instancia EC2
4. Spring Boot usa SDK AWS para obtener credenciales automáticamente

Flujo:
EC2 Instancia (con role asignado)
    │
    ├─ Requiere acceso a S3
    │
    ├─ Consulta Metadata Service: http://169.254.169.254/latest/meta-data/iam/info
    │
    ├─ AWS retorna: Token temporal (válido 6 horas)
    │
    ├─ S3: Aquí están mis credenciales
    │
    └─ S3: Acceso permitido ✓
```

### Principio de Menor Privilegio

```
NO HACER:
┌─────────────────────────────────┐
│ Rol: full-admin                 │
├─────────────────────────────────┤
│ Permisos: *:*                   │
│ (Acceso a TODO)                 │
└─────────────────────────────────┘
└─ Riesgo alto si credenciales se filtran

HACER:
┌─────────────────────────────────┐
│ Rol: ec2-galacticos             │
├─────────────────────────────────┤
│ Permisos:                       │
│ ├─ rds-db:connect               │
│ ├─ s3:GetObject (bucket xyz)    │
│ ├─ s3:PutObject (bucket xyz)    │
│ └─ cloudwatch:PutMetricData     │
└─────────────────────────────────┘
└─ Riesgo bajo, acceso limitado
```

---

## Almacenamiento S3

### ¿Qué es S3?

**S3 (Simple Storage Service)** = Almacenamiento de archivos ilimitado

```
EC2 Instance
│
├─ Almacenamiento local (EBS)
│  ├─ Rápido (SSD)
│  ├─ Limitado (máx 16 TB)
│  ├─ Caro
│  └─ Uso: SO, aplicación, BD
│
└─ S3 (Almacenamiento remoto)
   ├─ Lento (para archivos ocasionales)
   ├─ Ilimitado
   ├─ Barato ($0.023 por GB/mes)
   └─ Uso: Backups, uploads, archivos estáticos
```

### Estructura S3

```
AWS Account
│
└─ S3 Bucket: galacticos-uploads
   ├─ Region: us-east-1
   ├─ Versioning: Habilitado
   │
   └─ Objects:
      ├─ estudiantes/
      │  ├─ 1/
      │  │  ├─ profile.jpg (2 MB)
      │  │  └─ documento.pdf (500 KB)
      │  │
      │  └─ 2/
      │     └─ profile.jpg (2 MB)
      │
      └─ equipos/
         └─ 1/
            └─ foto.jpg (3 MB)
```

### Ubicación de Archivos Upload en Galacticos

```
Opción 1: EBS (En la instancia EC2)
/var/app/uploads/
├─ estudiantes/
│  ├─ 1/profile.jpg
│  └─ 2/profile.jpg
└─ equipos/
   └─ 1/foto.jpg

Ventajas:
├─ Acceso rápido
├─ URL simple: http://example.com/uploads/estudiantes/1/profile.jpg
└─ Sin costo adicional

Desventajas:
├─ Si EC2 falla, se pierden archivos
├─ No escalable (limitado a 16 TB)
└─ Difícil hacer backup

Opción 2: S3 (Almacenamiento AWS)
s3://galacticos-uploads/
├─ estudiantes/
│  ├─ 1/profile.jpg
│  └─ 2/profile.jpg
└─ equipos/
   └─ 1/foto.jpg

Ventajas:
├─ 99.99% disponibilidad
├─ Ilimitado
├─ Versionado
├─ Backup automático
└─ CDN integrado (CloudFront)

Desventajas:
├─ URL larga: https://galacticos-uploads.s3.amazonaws.com/...
├─ Latencia mayor
└─ Requiere SDK AWS en aplicación
```

### Integración Spring Boot + S3

```java
@Component
public class FileUploadService {
    
    @Autowired
    private AmazonS3 s3Client;
    
    public String uploadFile(MultipartFile file, String folder) {
        String key = folder + "/" + file.getOriginalFilename();
        
        // Subir a S3
        s3Client.putObject(
            new PutObjectRequest("galacticos-uploads", key, file.getInputStream(), 
                new ObjectMetadata())
        );
        
        // Generar URL pública
        URL url = s3Client.getUrl("galacticos-uploads", key);
        return url.toString();
    }
}
```

---

## Monitoreo y Alertas

### CloudWatch (Servicio de Monitoreo de AWS)

```
CloudWatch recopila:

EC2 Metrics:
├─ CPU Utilization (%)
├─ Network In/Out (bytes)
├─ Disk Read/Write (operaciones)
└─ Status Checks (system, instance)

RDS Metrics:
├─ CPU Utilization (%)
├─ Database Connections (número)
├─ Storage Space (bytes)
├─ Read/Write Latency (ms)
├─ IOPS (operaciones por segundo)
└─ Binary Log Disk Usage (bytes)

Application Logs:
├─ Spring Boot logs
├─ Nginx logs
├─ Sistema operativo logs
└─ Aplicación custom logs
```

### Dashboards CloudWatch

```
Galacticos Production Dashboard
┌──────────────────────────────────┐
│ EC2 CPU Utilization              │
│ ████░░░░ 45%                     │
├──────────────────────────────────┤
│ EC2 Memory Utilization           │
│ ██████░░░░ 65%                   │
├──────────────────────────────────┤
│ RDS CPU Utilization              │
│ ███░░░░░░░ 30%                   │
├──────────────────────────────────┤
│ RDS Connections (45/100)         │
│ ████░░░░░░ 45%                   │
├──────────────────────────────────┤
│ Network In: 250 MB/h             │
│ Network Out: 150 MB/h            │
├──────────────────────────────────┤
│ Application Errors: 2 (últimas h) │
└──────────────────────────────────┘
```

### Alarmas (Alerts)

```
Alarma: EC2 CPU > 80%
├─ Métrica: CPU Utilization (EC2)
├─ Threshold: > 80%
├─ Period: 5 minutos
├─ Evaluations: 2 (debe estar > 80% por 10 min)
├─ Acciones:
│  ├─ Enviar email a ops@example.com
│  ├─ SMS a +1234567890
│  └─ Ejecutar Lambda para auto-scaling
└─ Historial: (gráfico de alertas)

Estados:
├─ OK: Métrica normal
├─ ALARM: Métrica excedió threshold
├─ INSUFFICIENT_DATA: No hay datos aún
└─ UNKNOWN: Error
```

### Logs CloudWatch

```
CloudWatch Logs:
├─ Log Group: /aws/ec2/galacticos
│  ├─ Log Stream: i-0d12345ab6c789ef (Instancia)
│  │  ├─ 2024-02-04 10:15:23 INFO Starting application
│  │  ├─ 2024-02-04 10:15:25 INFO Connecting to RDS
│  │  ├─ 2024-02-04 10:15:26 INFO Server started on port 8080
│  │  ├─ 2024-02-04 10:16:00 ERROR Database connection failed
│  │  └─ 2024-02-04 10:16:01 INFO Retrying connection
│  │
│  └─ Log Stream: i-0e98765zyx654321 (Otra instancia)
│     └─ ...
│
└─ Log Group: /aws/rds/instance/galacticos-db
   └─ Error log
   └─ Slow query log
   └─ General log
```

### Log Insights (Búsqueda avanzada)

```sql
-- Encontrar errores en la última hora
fields @timestamp, @message
| filter @message like /ERROR/
| stats count() by @message

-- Latencia promedio por endpoint
fields @duration, @path
| stats avg(@duration) as avg_duration by @path
| sort avg_duration desc

-- Errores 500 (Server Error)
fields @timestamp, @path, @status
| filter @status >= 500
| stats count() as error_count by @path
```

---

## Costos y Optimización

### Desglose de Costos Mensuales

```
┌─────────────────────────────┐
│  t3.micro (1 año reservado) │
│  CPU: 1 vCPU                │
│  RAM: 1 GB                  │
│  Almacenamiento: 30 GB EBS  │
│  On-Demand: $7.50/mes       │
│  Reserved (1 año): $5/mes   │
│  Ahorro: 33%                │
├─────────────────────────────┤
│  Transferencia de Datos     │
│  0-1 GB: Gratis             │
│  1-10 TB: $0.09/GB          │
│  Estimado: $5/mes           │
├─────────────────────────────┤
│  db.t3.micro RDS            │
│  On-Demand: $25/mes         │
│  Almacenamiento: $2.30/mes  │
│  Backups: Gratis (7 días)   │
│  Total: $27.30/mes          │
├─────────────────────────────┤
│  CloudWatch                 │
│  Primeros 10 alarmas: Gratis│
│  Logs: $0.50/GB ingestados  │
│  Estimado: $5/mes           │
├─────────────────────────────┤
│  Otros (DNS, SSL, etc)      │
│  Estimado: $3/mes           │
├─────────────────────────────┤
│  TOTAL ESTIMADO: $45-50/mes │
└─────────────────────────────┘

CAPA GRATUITA AWS (12 meses):
✓ EC2 t2.micro 750h/mes
✓ RDS db.t2.micro 750h/mes
✓ 5 GB almacenamiento S3
✓ 20 GB transferencia datos
└─ COSTO: $0/mes (si no excedes límites)
```

### Optimización de Costos

```
1. Usar Instancias Reservadas
   └─ Comprometer 1-3 años para 30-50% descuento

2. Apagar instancias no usadas
   └─ Desarrollo: apagar noches y fines de semana
   └─ Ahorro: 50-70%

3. Usar Spot Instances
   └─ EC2 spot: hasta 90% descuento
   └─ Interrupciones ocasionales (no apto para prod)

4. Consolidated Billing
   └─ Múltiples cuentas AWS
   └─ Mayor descuento por volumen

5. Auto-Scaling
   └─ Subir/bajar recursos automáticamente
   └─ Pagar solo lo que usas

6. Usar Free Tier
   └─ t2.micro/t3.micro gratis 1 año
   └─ RDS mysql.t2.micro gratis 1 año
   └─ 750 horas mensuales

7. Monitoreo de Costos
   └─ AWS Budgets: alerta si gastos exceden límite
   └─ Cost Explorer: visualizar gastos por servicio
```

### AWS Budgets (Control de Gastos)

```
Budget: Monthly Limit
├─ Límite: $50/mes
├─ Período: Enero - Diciembre
├─ Servicios: EC2, RDS, CloudWatch
│
├─ Alertas:
│  ├─ 50% de presupuesto: Email a ops@example.com
│  ├─ 80% de presupuesto: SMS + Email
│  └─ 100% de presupuesto: Email urgente
│
└─ Si exceedes:
   ├─ Tu cuenta no se suspende automáticamente
   ├─ Debes monitorear y actuar
   └─ Recomendación: Apagarla manualmente
```

---

## Flujo Completo de una Solicitud

### Ejemplo: Usuario descarga fotogrado de estudiante

```
1. Usuario abre navegador
   └─ URL: https://galacticos.com/api/estudiante/1/foto

2. DNS Resolution
   └─ Navegador: ¿Dónde está galacticos.com?
   └─ Route 53 (DNS AWS): 203.0.113.45 (Elastic IP)

3. TLS Handshake (Establecer conexión segura)
   └─ Navegador ↔ Nginx en EC2
   └─ Verificar certificado SSL
   └─ Generar clave de sesión encriptada

4. HTTP Request
   └─ GET /api/estudiante/1/foto HTTP/1.1
   └─ Host: galacticos.com
   └─ Authorization: Bearer eyJhbG...

5. Nginx (Reverse Proxy)
   └─ Recibe solicitud HTTPS en puerto 443
   └─ Descencripta con SSL
   └─ Redirige HTTP a localhost:8080
   └─ GET http://localhost:8080/api/estudiante/1/foto

6. Spring Boot (Aplicación)
   └─ @GetMapping("/api/estudiante/{id}/foto")
   └─ Autenticar usuario (JWT)
   └─ Query a BD: SELECT foto_url FROM estudiantes WHERE id=1
   └─ RDS ejecuta query

7. RDS (Base de Datos)
   └─ Query recibida en puerto 3306
   └─ Busca fila en tabla estudiantes
   └─ Retorna: foto_url = "uploads/estudiantes/1/profile.jpg"

8. Spring Boot (Continúa)
   └─ Leer archivo: /var/app/uploads/estudiantes/1/profile.jpg (EBS)
   └─ O descargar de S3: s3://galacticos-uploads/estudiantes/1/profile.jpg
   └─ Crear response JSON:
      {
        "id": 1,
        "nombre": "Juan",
        "fotoUrl": "https://galacticos.s3.amazonaws.com/estudiantes/1/profile.jpg"
      }

9. Nginx (Reverse Proxy)
   └─ Recibe respuesta HTTP de Spring Boot
   └─ Encripta con SSL
   └─ Envía HTTPS

10. Navegador
    └─ Descifra respuesta
    └─ Descarga imagen de URL
    └─ Muestra foto al usuario
```

### Tiempos de Latencia Estimados

```
Paso 1: DNS          5 ms      ████
Paso 2: TLS          50 ms     ████████████████████████
Paso 3: HTTP Request 10 ms     █████
Paso 4: Nginx        5 ms      ███
Paso 5: Spring Boot  30 ms     ███████████████
Paso 6: RDS Query    20 ms     ██████████
Paso 7: EBS Read     10 ms     █████
Paso 8: Nginx        5 ms      ███
Paso 9: Transfer     50 ms     ████████████████████████
         ────────────
Total:   185 ms      BUENO (< 500 ms es aceptable)
```

---

## Checklist de Configuración AWS

- [ ] **VPC y Networking**
  - [ ] VPC creada (10.0.0.0/16)
  - [ ] Subnet pública (10.0.1.0/24)
  - [ ] Subnet privada (10.0.2.0/24)
  - [ ] Internet Gateway conectado
  - [ ] Route tables configuradas

- [ ] **EC2**
  - [ ] Instancia t3.micro/t3.small creada
  - [ ] Security Group configurado (SSH, HTTP, HTTPS)
  - [ ] Elastic IP asignada
  - [ ] IAM Role asignado
  - [ ] EBS encriptado
  - [ ] Monitoring habilitado

- [ ] **RDS**
  - [ ] Instancia db.t3.micro creada
  - [ ] Security Group configurado (3306)
  - [ ] BD "escuela_voleibol" creada
  - [ ] Backups automáticos habilitados (7 días)
  - [ ] Multi-AZ habilitado (producción)
  - [ ] Enhanced monitoring habilitado

- [ ] **Seguridad**
  - [ ] IAM roles configurados
  - [ ] Políticas con menor privilegio
  - [ ] Credenciales root no usadas
  - [ ] MFA habilitado en cuenta principal

- [ ] **Aplicación**
  - [ ] application-prod.properties configurado
  - [ ] RDS endpoint actualizado
  - [ ] JWT secret seguro
  - [ ] Wompi keys en producción

- [ ] **SSL/TLS**
  - [ ] Certificado Let's Encrypt obtenido
  - [ ] Nginx configurado como reverse proxy
  - [ ] HTTPS redirige desde HTTP
  - [ ] Certificado se renueva automáticamente

- [ ] **Monitoreo**
  - [ ] CloudWatch alarmas creadas
  - [ ] Logs configurados
  - [ ] Health checks habilitados
  - [ ] Backups de BD configurados

---

## Conclusión

AWS es un ecosistema complejo pero poderoso. Para Galacticos:

**Arquitectura Recomendada:**
```
┌─────────────────────────────────────────┐
│  Route 53 (DNS)                         │
│  galacticos.com → Elastic IP            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  EC2 (Ubuntu + Nginx + Spring Boot)     │
│  t3.small | 2GB RAM | 30GB SSD          │
│  Security Group: SSH, HTTP, HTTPS       │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  RDS MySQL (Capa Privada)               │
│  db.t3.micro | Multi-AZ                 │
│  Backups: 7 días                        │
└─────────────────────────────────────────┘

Costo: $45-60/mes (producción)
Capa Gratuita: $0/mes (1 año, desarrollo)
```

**Próximos Pasos:**
1. Crear cuenta AWS
2. Configurar VPC y subnets
3. Crear RDS MySQL
4. Crear EC2 + Elastic IP
5. Desplegar aplicación
6. Configurar dominio y SSL
7. Monitoreo y alertas
8. Backup y recuperación

---

**Versión:** 2.0  
**Última actualización:** Febrero 2026  
**Nivel:** Intermedio
