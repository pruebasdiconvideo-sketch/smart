-- =====================================================================
-- Smart Power On S.A.C. - DDL: 16 tablas normalizadas en 3FN (Informe §7.3)
-- Motor: MySQL 8.0  ·  Compatible con H2 en modo MySQL
-- ---------------------------------------------------------------------
-- Para ejecución manual en MySQL, crear primero la base de datos:
--   CREATE DATABASE smart_power_on_db CHARACTER SET utf8mb4;
--   USE smart_power_on_db;
-- Ejecutar manualmente sólo al crear una base de datos nueva.
-- =====================================================================

-- Reseteo defensivo (idempotente, sólo cuando se ejecuta desde cero)
DROP TABLE IF EXISTS trazabilidad;
DROP TABLE IF EXISTS material_instancia;
DROP TABLE IF EXISTS merma_devolucion;
DROP TABLE IF EXISTS prueba_calidad;
DROP TABLE IF EXISTS parametro_prueba;
DROP TABLE IF EXISTS entrega;
DROP TABLE IF EXISTS detalle_despacho;
DROP TABLE IF EXISTS despacho;
DROP TABLE IF EXISTS detalle_solicitud;
DROP TABLE IF EXISTS solicitud;
DROP TABLE IF EXISTS detalle_orden_compra;
DROP TABLE IF EXISTS orden_compra;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS proyecto;
DROP TABLE IF EXISTS empresa;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol_permiso;
DROP TABLE IF EXISTS permiso;
DROP TABLE IF EXISTS rol;

-- =====================================================================
-- 1. TABLAS DE ACCESO (A0)
-- =====================================================================
CREATE TABLE rol (
    id_rol        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(50)  UNIQUE NOT NULL,
    descripcion   TEXT
);

CREATE TABLE permiso (
    id_permiso    BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(50)  UNIQUE NOT NULL,
    descripcion   TEXT
);

CREATE TABLE rol_permiso (
    id_rol        BIGINT NOT NULL,
    id_permiso    BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol)     REFERENCES rol(id_rol)     ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso) ON DELETE CASCADE
);

CREATE TABLE usuario (
    id_usuario            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_rol                BIGINT NOT NULL,
    nombre_completo       VARCHAR(150) NOT NULL,
    correo                VARCHAR(150) UNIQUE NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    activo                BOOLEAN DEFAULT TRUE,
    ultimo_acceso         DATETIME,
    FOREIGN KEY (id_rol)  REFERENCES rol(id_rol)
);

-- =====================================================================
-- 2. TABLAS MAESTRAS Y PROYECTOS (P1, P2)
-- =====================================================================
CREATE TABLE empresa (
    id_empresa          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ruc                 VARCHAR(11)  UNIQUE NOT NULL,
    razon_social        VARCHAR(180) NOT NULL,
    razon_comercial     VARCHAR(180),
    contacto_nombre     VARCHAR(150),
    contacto_telefono   VARCHAR(30),
    contacto_correo     VARCHAR(120),
    tipo                VARCHAR(15)  NOT NULL CHECK (tipo IN ('PROVEEDOR','CLIENTE')),
    estado              VARCHAR(20)  DEFAULT 'ACTIVO',
    fecha_registro      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proyecto (
    id_proyecto   BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(50)  UNIQUE NOT NULL,
    cliente       VARCHAR(150) NOT NULL,
    id_empresa    BIGINT,
    fecha_inicio  DATE,
    presupuesto   DECIMAL(12,2) NOT NULL CHECK (presupuesto > 0),
    estado        VARCHAR(30)  DEFAULT 'EN_PROCESO',
    FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa)
);

CREATE TABLE material (
    id_material          BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo               VARCHAR(50)  UNIQUE NOT NULL,
    nombre               VARCHAR(150) NOT NULL,
    especificacion_tecnica TEXT,
    descripcion          TEXT,
    categoria            VARCHAR(50),
    marca                VARCHAR(50),
    unidad_medida        VARCHAR(20),
    modelo               VARCHAR(100),
    mlfb                 VARCHAR(100),
    pasillo              VARCHAR(20),
    estante              VARCHAR(20),
    nivel                VARCHAR(20),
    estado               VARCHAR(20) DEFAULT 'ACTIVO',
    precio_unitario      DECIMAL(12,2) NOT NULL,
    stock_actual         INT DEFAULT 0 CHECK (stock_actual >= 0), -- R-INV-03
    stock_minimo         INT DEFAULT 0
);

CREATE TABLE orden_compra (
    id_orden_compra  BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo           VARCHAR(50)  UNIQUE NOT NULL,
    proveedor        VARCHAR(150) NOT NULL,
    id_empresa       BIGINT,
    fecha_emision    DATE NOT NULL,
    estado           VARCHAR(30)  DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA')),
    FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa)
);

CREATE TABLE detalle_orden_compra (
    id_detalle_oc     BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_orden_compra   BIGINT NOT NULL,
    id_material       BIGINT NOT NULL,
    cantidad          INT NOT NULL CHECK (cantidad > 0),
    precio_unitario   DECIMAL(12,2),
    FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
    FOREIGN KEY (id_material)     REFERENCES material(id_material)
);

-- =====================================================================
-- 3. TABLAS DE SOLICITUD Y DESPACHO (P2, P3)
-- =====================================================================
CREATE TABLE solicitud (
    id_solicitud            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto             BIGINT NOT NULL,
    id_usuario_solicitante  BIGINT NOT NULL,
    id_usuario_aprobador    BIGINT,
    fecha_solicitud         DATETIME DEFAULT CURRENT_TIMESTAMP,
    costo_total             DECIMAL(12,2) DEFAULT 0.00,
    anulada                 BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion        TEXT,
    estado                  VARCHAR(30)  DEFAULT 'BORRADOR'
        CHECK (estado IN ('BORRADOR', 'RETENIDA', 'RECHAZADA', 'LISTA_DESPACHO', 'DESPACHADA')),
    FOREIGN KEY (id_proyecto)            REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_usuario_solicitante) REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_usuario_aprobador)   REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_solicitud (
    id_detalle_solicitud  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud          BIGINT NOT NULL,
    id_material           BIGINT NOT NULL,
    cantidad_solicitada   INT NOT NULL CHECK (cantidad_solicitada > 0),
    cantidad_despachada   INT DEFAULT 0   CHECK (cantidad_despachada >= 0),
    FOREIGN KEY (id_solicitud) REFERENCES solicitud(id_solicitud),
    FOREIGN KEY (id_material)  REFERENCES material(id_material)
);

CREATE TABLE despacho (
    id_despacho            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud           BIGINT NOT NULL,
    id_usuario_almacenero  BIGINT NOT NULL,
    fecha_despacho         DATETIME DEFAULT CURRENT_TIMESTAMP,
    tipo                   VARCHAR(20) CHECK (tipo IN ('COMPLETO', 'PARCIAL')),
    estado                 VARCHAR(30) DEFAULT 'PENDIENTE_CONFIRMACION'
        CHECK (estado IN ('PENDIENTE_CONFIRMACION', 'CONFIRMADO')),
    anulado                BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion       TEXT,
    FOREIGN KEY (id_solicitud)          REFERENCES solicitud(id_solicitud),
    FOREIGN KEY (id_usuario_almacenero) REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_despacho (
    id_detalle_despacho  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_despacho          BIGINT NOT NULL,
    id_detalle_solicitud BIGINT NOT NULL,
    id_material          BIGINT NOT NULL,
    codigo_digitado      VARCHAR(50),
    cantidad_entregada   INT NOT NULL CHECK (cantidad_entregada > 0),
    FOREIGN KEY (id_despacho)          REFERENCES despacho(id_despacho),
    FOREIGN KEY (id_detalle_solicitud) REFERENCES detalle_solicitud(id_detalle_solicitud),
    FOREIGN KEY (id_material)          REFERENCES material(id_material)
);

CREATE TABLE entrega (
    id_entrega           BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_despacho          BIGINT UNIQUE NOT NULL,
    id_usuario_operario  BIGINT NOT NULL,
    fecha_confirmacion   DATETIME DEFAULT CURRENT_TIMESTAMP,
    confirmado           BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_despacho)         REFERENCES despacho(id_despacho),
    FOREIGN KEY (id_usuario_operario) REFERENCES usuario(id_usuario)
);

-- =====================================================================
-- 4. CALIDAD Y TRAZABILIDAD (P4, P5)
-- =====================================================================
CREATE TABLE parametro_prueba (
    id_parametro       BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_prueba        VARCHAR(50),
    unidad             VARCHAR(20),
    valor_minimo       DECIMAL(10,3),
    valor_maximo       DECIMAL(10,3),
    norma_referencia   VARCHAR(150),
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_valores CHECK (valor_maximo >= valor_minimo)
);

CREATE TABLE prueba_calidad (
    id_prueba            BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto          BIGINT NOT NULL,
    id_parametro         BIGINT NOT NULL,
    id_usuario_inspector BIGINT NOT NULL,
    valor_medido         DECIMAL(10,3),
    resultado            VARCHAR(20) CHECK (resultado IN ('CONFORME', 'NO_CONFORME')),
    observacion          TEXT,
    fecha_prueba         DATETIME DEFAULT CURRENT_TIMESTAMP,
    anulada              BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion     TEXT,
    tecnico_externo      VARCHAR(150),
    FOREIGN KEY (id_proyecto)          REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_parametro)         REFERENCES parametro_prueba(id_parametro),
    FOREIGN KEY (id_usuario_inspector) REFERENCES usuario(id_usuario)
);

CREATE TABLE merma_devolucion (
    id_merma              BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proyecto           BIGINT NOT NULL,
    id_material           BIGINT NOT NULL,
    id_usuario_registra   BIGINT NOT NULL,
    id_usuario_validador  BIGINT,
    tipo                  VARCHAR(20) CHECK (tipo IN ('DEVOLUCION', 'MERMA')),
    cantidad              INT NOT NULL CHECK (cantidad > 0),
    motivo                TEXT,
    estado                VARCHAR(20) DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'REINTEGRADO', 'MERMADO')),
    fecha                 DATETIME DEFAULT CURRENT_TIMESTAMP,
    anulada               BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion      TEXT,
    FOREIGN KEY (id_proyecto)          REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_material)          REFERENCES material(id_material),
    FOREIGN KEY (id_usuario_registra)  REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_usuario_validador) REFERENCES usuario(id_usuario)
);

-- Trazabilidad: SOLO INSERCIÓN (RNF07 / P5-RN03). La aplicación no expone
-- UPDATE ni DELETE sobre ella. En MySQL un trigger refuerza la regla.
CREATE TABLE trazabilidad (
    id_trazabilidad   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_material       BIGINT NOT NULL,
    id_usuario        BIGINT NOT NULL,
    id_proyecto       BIGINT,
    id_orden_compra   BIGINT,
    tipo_movimiento   VARCHAR(30) CHECK (tipo_movimiento IN ('ENTRADA','SALIDA','DEVOLUCION','MERMA')),
    cantidad          INT NOT NULL CHECK (cantidad > 0),
    fecha_movimiento  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_material)     REFERENCES material(id_material),
    FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_proyecto)     REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra)
);

-- Instancia física de un material (una unidad) identificada por número de serie único.
-- Se crea al ingresar material contra una OC; da trazabilidad individual por unidad.
-- El stock consolidado por modelo/código/marca vive en material.stock_actual.
CREATE TABLE material_instancia (
    id_instancia       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_material        BIGINT NOT NULL,
    numero_serie       VARCHAR(100) NOT NULL,
    id_orden_compra    BIGINT,
    fecha_ingreso      DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario         BIGINT,
    estado             VARCHAR(20) DEFAULT 'EN_ALMACEN' CHECK (estado IN ('EN_ALMACEN','SALIDA','MERMADO')),
    UNIQUE KEY uk_instancia_numero_serie (numero_serie),
    FOREIGN KEY (id_material)     REFERENCES material(id_material),
    FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
    FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario)
);

-- =====================================================================
-- 16 tablas · 26 claves foráneas · esquema snake_case MySQL
-- =====================================================================
