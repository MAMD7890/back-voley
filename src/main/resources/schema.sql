-- ========================================
-- BASE DE DATOS ESCUELA DE VOLEIBOL
-- GENERADA AUTOMÁTICAMENTE POR HIBERNATE
-- ========================================
-- Este archivo es una referencia del script SQL original
-- Las tablas se crean automáticamente desde las entidades JPA

CREATE DATABASE IF NOT EXISTS escuela_voleibol;
USE escuela_voleibol;

-- =========================
-- ROLES Y USUARIOS
-- =========================

CREATE TABLE rol (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);

-- Insertar roles por defecto
INSERT INTO rol (nombre) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE nombre = nombre;
INSERT INTO rol (nombre) VALUES ('USER') ON DUPLICATE KEY UPDATE nombre = nombre;
INSERT INTO rol (nombre) VALUES ('PROFESOR') ON DUPLICATE KEY UPDATE nombre = nombre;
INSERT INTO rol (nombre) VALUES ('STUDENT') ON DUPLICATE KEY UPDATE nombre = nombre;

CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    foto_url VARCHAR(255),
    foto_nombre VARCHAR(100),
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(50),
    telefono VARCHAR(20),
    estado BOOLEAN DEFAULT TRUE,
    id_rol INT,
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

-- =========================
-- SEDES
-- =========================

CREATE TABLE sede (
    id_sede INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    direccion VARCHAR(200),
    telefono VARCHAR(20),
    estado BOOLEAN DEFAULT TRUE
);

-- =========================
-- CATEGORÍAS
-- =========================

CREATE TABLE categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50),
    rango_edad VARCHAR(50),
    descripcion TEXT
);

-- =========================
-- PROFESORES
-- =========================

CREATE TABLE profesor (
    id_profesor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    documento VARCHAR(50),
    telefono VARCHAR(20),
    correo VARCHAR(100),
    salario_por_clase DECIMAL(10,2),
    foto_url VARCHAR(255),
    foto_nombre VARCHAR(100),
    estado BOOLEAN DEFAULT TRUE
);

-- =========================
-- EQUIPOS
-- =========================

CREATE TABLE equipo (
    id_equipo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    id_categoria INT,
    id_sede INT,
    horario VARCHAR(100),
    foto_url VARCHAR(255),
    estado BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria),
    FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
);

-- =========================
-- PROFESOR ⇄ EQUIPO (N:M)
-- =========================

CREATE TABLE profesor_equipo (
    id_profesor_equipo INT AUTO_INCREMENT PRIMARY KEY,
    id_profesor INT NOT NULL,
    id_equipo INT NOT NULL,
    rol ENUM('PRINCIPAL','ASISTENTE') DEFAULT 'PRINCIPAL',
    FOREIGN KEY (id_profesor) REFERENCES profesor(id_profesor),
    FOREIGN KEY (id_equipo) REFERENCES equipo(id_equipo),
    UNIQUE (id_profesor, id_equipo)
);

-- =========================
-- ESTUDIANTES
-- =========================

CREATE TABLE estudiante (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(200) NOT NULL,
    tipo_documento ENUM('TI','CC','RC','PASAPORTE') NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    edad INT NOT NULL,
    sexo ENUM('MASCULINO','FEMENINO','OTRO'),
    direccion_residencia VARCHAR(200),
    barrio VARCHAR(100),
    celular_estudiante VARCHAR(20),
    whatsapp_estudiante VARCHAR(20),
    correo_estudiante VARCHAR(100),
    id_sede INT NOT NULL,
    nombre_tutor VARCHAR(200),
    parentesco_tutor VARCHAR(50),
    documento_tutor VARCHAR(20),
    telefono_tutor VARCHAR(20),
    correo_tutor VARCHAR(100),
    ocupacion_tutor VARCHAR(100),
    institucion_educativa VARCHAR(150),
    jornada ENUM('MAÑANA','TARDE','NOCHE','UNICA'),
    grado_actual INT,
    eps VARCHAR(100),
    tipo_sangre VARCHAR(20),
    alergias TEXT,
    enfermedades_condiciones TEXT,
    medicamentos TEXT,
    certificado_medico_deportivo BOOLEAN DEFAULT FALSE,
    dia_pago_mes INT,
    nombre_emergencia VARCHAR(200),
    telefono_emergencia VARCHAR(20),
    parentesco_emergencia VARCHAR(50),
    ocupacion_emergencia VARCHAR(100),
    correo_emergencia VARCHAR(100),
    pertenece_lgbtiq BOOLEAN DEFAULT FALSE,
    persona_discapacidad BOOLEAN DEFAULT FALSE,
    condicion_discapacidad TEXT,
    migrante_refugiado BOOLEAN DEFAULT FALSE,
    poblacion_etnica VARCHAR(100),
    religion VARCHAR(100),
    experiencia_voleibol TEXT,
    otras_disciplinas TEXT,
    posicion_preferida VARCHAR(50),
    dominancia ENUM('DERECHA','IZQUIERDA','AMBIDIESTRO'),
    nivel_actual ENUM('INICIANTE','INTERMEDIO','AVANZADO'),
    clubes_anteriores TEXT,
    acepta_consentimiento BOOLEAN DEFAULT FALSE,
    firma_digital VARCHAR(200),
    fecha_diligenciamiento DATE,
    nombre_camiseta VARCHAR(50),
    numero_camiseta INT,
    foto_url VARCHAR(255),
    foto_nombre VARCHAR(100),
    estado BOOLEAN DEFAULT TRUE,
    estado_pago ENUM('PENDIENTE','AL_DIA','EN_MORA','COMPROMISO_PAGO') DEFAULT 'PENDIENTE',
    FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
);

-- =========================
-- MEMBRESÍAS
-- =========================

CREATE TABLE membresia (
    id_membresia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT,
    id_equipo INT,
    fecha_inicio DATE,
    fecha_fin DATE,
    valor_mensual DECIMAL(10,2),
    estado ENUM('VIGENTE','VENCIDA'),
    FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
    FOREIGN KEY (id_equipo) REFERENCES equipo(id_equipo)
);

-- =========================
-- PAGOS
-- =========================

CREATE TABLE pago (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT,
    mes_pagado VARCHAR(20),
    valor DECIMAL(10,2),
    metodo_pago ENUM('ONLINE','EFECTIVO'),
    referencia_pago VARCHAR(100),
    fecha_pago DATE,
    hora_pago TIME,
    estado_pago ENUM('PAGADO','PENDIENTE','VENCIDO'),
    FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante)
);

-- =========================
-- ASISTENCIA ESTUDIANTES
-- =========================

CREATE TABLE asistencia_estudiante (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT,
    id_equipo INT,
    fecha DATE,
    asistio BOOLEAN,
    observaciones VARCHAR(200),
    FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante),
    FOREIGN KEY (id_equipo) REFERENCES equipo(id_equipo)
);

-- =========================
-- ASISTENCIA PROFESORES
-- =========================

CREATE TABLE asistencia_profesor (
    id_asistencia_profesor INT AUTO_INCREMENT PRIMARY KEY,
    id_profesor INT,
    id_equipo INT,
    fecha DATE,
    horas_trabajadas DECIMAL(5,2),
    FOREIGN KEY (id_profesor) REFERENCES profesor(id_profesor),
    FOREIGN KEY (id_equipo) REFERENCES equipo(id_equipo)
);

-- =========================
-- GASTOS
-- =========================

CREATE TABLE gasto (
    id_gasto INT AUTO_INCREMENT PRIMARY KEY,
    concepto VARCHAR(100),
    descripcion TEXT,
    monto DECIMAL(10,2),
    fecha DATE,
    id_sede INT,
    FOREIGN KEY (id_sede) REFERENCES sede(id_sede)
);

-- =========================
-- RECORDATORIOS DE PAGO (WHATSAPP)
-- =========================

CREATE TABLE recordatorio_pago (
    id_recordatorio INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT,
    fecha_envio DATETIME,
    mensaje VARCHAR(255),
    estado ENUM('ENVIADO','PENDIENTE'),
    FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante)
);
