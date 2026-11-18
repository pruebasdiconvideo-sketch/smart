-- =====================================================================
-- Smart Power On S.A.C. - Migración consolidada para base existente
-- =====================================================================
-- APTO PARA: una base MySQL que ya tenga el esquema base cargado
--            (por ejemplo, desde `basecompleta.sql`).
--
-- Qué hace:
--   * Añade las columnas de borrado lógico (anulada / motivo_anulacion)
--     a solicitud, despacho, prueba_calidad y merma_devolucion.
--   * Añade la columna `activo` a parametro_prueba.
--   * Añade (si faltaran) las columnas extra de material
--     (descripcion, marca, modelo, mlfb, pasillo, etc.).
--
-- Es 100% IDEMPOTENTE: cada ALTER se ejecuta sólo si la columna no
-- existe aún (verificación contra INFORMATION_SCHEMA). Puede correrse
-- cuantas veces sea necesario sin perder datos.
--
-- USO:
--   mysql -u root -p smart_power_on_db < MIGRAR_BASE_EXISTENTE.sql
--
-- No toca los stored procedures existentes (sp_ConfirmarEntregaYDescontar,
-- sp_RegistrarIngresoMaterial, sp_ValidarMermaDevolucion) ni los datos.
-- =====================================================================

USE smart_power_on_db;

-- Procedimiento temporal que agrupa todas las verificaciones
DROP PROCEDURE IF EXISTS sp_migrar_base_existente;
DELIMITER $$
CREATE PROCEDURE sp_migrar_base_existente()
BEGIN
    DECLARE msg TEXT;
    -- Tolerar tablas/columnas inexistentes sin abortar toda la migración
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;

    -- -----------------------------------------------------------------
    -- 1. MATERIAL: columnas extra (descripcion, marca, modelo, mlfb, ...)
    --    (normalmente ya existen si la base viene de basecompleta.sql)
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'descripcion') THEN
        ALTER TABLE material ADD COLUMN descripcion TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'marca') THEN
        ALTER TABLE material ADD COLUMN marca VARCHAR(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'unidad_medida') THEN
        ALTER TABLE material ADD COLUMN unidad_medida VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'modelo') THEN
        ALTER TABLE material ADD COLUMN modelo VARCHAR(100);
    END IF;
    -- El catálogo de modelos ya no lleva número de serie (la serie es por unidad,
    -- en material_instancia). Eliminar columnas legacy numero_serie/numero_modelo si existen.
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'numero_serie') THEN
        ALTER TABLE material DROP COLUMN numero_serie;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'numero_modelo') THEN
        ALTER TABLE material DROP COLUMN numero_modelo;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'mlfb') THEN
        ALTER TABLE material ADD COLUMN mlfb VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'pasillo') THEN
        ALTER TABLE material ADD COLUMN pasillo VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'estante') THEN
        ALTER TABLE material ADD COLUMN estante VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'nivel') THEN
        ALTER TABLE material ADD COLUMN nivel VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'estado') THEN
        ALTER TABLE material ADD COLUMN estado VARCHAR(20) DEFAULT 'ACTIVO';
    END IF;

    -- -----------------------------------------------------------------
    -- 2. PARAMETRO_PRUEBA: columna `activo` (borrado lógico)
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'parametro_prueba'
          AND COLUMN_NAME = 'activo'
    ) THEN
        ALTER TABLE parametro_prueba
            ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    -- -----------------------------------------------------------------
    -- 3. SOLICITUD: anulada + motivo_anulacion
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud' AND COLUMN_NAME = 'anulada'
    ) THEN
        ALTER TABLE solicitud ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud' AND COLUMN_NAME = 'motivo_anulacion'
    ) THEN
        ALTER TABLE solicitud ADD COLUMN motivo_anulacion TEXT;
    END IF;

    -- -----------------------------------------------------------------
    -- 4. DESPACHO: anulado + motivo_anulacion
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'despacho' AND COLUMN_NAME = 'anulado'
    ) THEN
        ALTER TABLE despacho ADD COLUMN anulado BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'despacho' AND COLUMN_NAME = 'motivo_anulacion'
    ) THEN
        ALTER TABLE despacho ADD COLUMN motivo_anulacion TEXT;
    END IF;

    -- -----------------------------------------------------------------
    -- 5. PRUEBA_CALIDAD: anulada + motivo_anulacion
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prueba_calidad' AND COLUMN_NAME = 'anulada'
    ) THEN
        ALTER TABLE prueba_calidad ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prueba_calidad' AND COLUMN_NAME = 'motivo_anulacion'
    ) THEN
        ALTER TABLE prueba_calidad ADD COLUMN motivo_anulacion TEXT;
    END IF;

    -- -----------------------------------------------------------------
    -- 6. MERMA_DEVOLUCION: anulada + motivo_anulacion
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merma_devolucion' AND COLUMN_NAME = 'anulada'
    ) THEN
        ALTER TABLE merma_devolucion ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merma_devolucion' AND COLUMN_NAME = 'motivo_anulacion'
    ) THEN
        ALTER TABLE merma_devolucion ADD COLUMN motivo_anulacion TEXT;
    END IF;

    -- -----------------------------------------------------------------
    -- 7. EMPRESA: tabla de proveedores y clientes (nueva)
    -- -----------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS empresa (
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

    -- -----------------------------------------------------------------
    -- 8. DETALLE_ORDEN_COMPRA: ítems de la OC (varios materiales, varias categorías)
    -- -----------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS detalle_orden_compra (
        id_detalle_oc     BIGINT AUTO_INCREMENT PRIMARY KEY,
        id_orden_compra   BIGINT NOT NULL,
        id_material       BIGINT NOT NULL,
        cantidad          INT NOT NULL CHECK (cantidad > 0),
        precio_unitario   DECIMAL(12,2),
        FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
        FOREIGN KEY (id_material)     REFERENCES material(id_material)
    );

    -- -----------------------------------------------------------------
    -- 8b. MATERIAL_INSTANCIA: una fila por unidad física ingresada al almacén,
    --     identificada por su número de serie único (trazabilidad individual).
    --     El stock consolidado por modelo/código/marca vive en material.stock_actual.
    -- -----------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS material_instancia (
        id_instancia       BIGINT AUTO_INCREMENT PRIMARY KEY,
        id_material        BIGINT NOT NULL,
        numero_serie       VARCHAR(100) NOT NULL,
        id_orden_compra    BIGINT,
        fecha_ingreso      DATETIME DEFAULT CURRENT_TIMESTAMP,
        id_usuario         BIGINT,
        estado             VARCHAR(20) DEFAULT 'EN_ALMACEN',
        UNIQUE KEY uk_instancia_numero_serie (numero_serie),
        FOREIGN KEY (id_material)     REFERENCES material(id_material),
        FOREIGN KEY (id_orden_compra) REFERENCES orden_compra(id_orden_compra),
        FOREIGN KEY (id_usuario)      REFERENCES usuario(id_usuario)
    );

    -- -----------------------------------------------------------------
    -- 9. ORDEN_COMPRA: id_empresa (FK al proveedor registrado)
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orden_compra' AND COLUMN_NAME = 'id_empresa'
    ) THEN
        ALTER TABLE orden_compra ADD COLUMN id_empresa BIGINT;
        ALTER TABLE orden_compra ADD FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa);
    END IF;

    -- -----------------------------------------------------------------
    -- 10. PROYECTO: id_empresa (FK al cliente registrado)
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'proyecto' AND COLUMN_NAME = 'id_empresa'
    ) THEN
        ALTER TABLE proyecto ADD COLUMN id_empresa BIGINT;
        ALTER TABLE proyecto ADD FOREIGN KEY (id_empresa) REFERENCES empresa(id_empresa);
    END IF;

    -- -----------------------------------------------------------------
    -- 11. PRUEBA_CALIDAD: tecnico_externo (nombre del técnico que realiza la prueba)
    -- -----------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prueba_calidad' AND COLUMN_NAME = 'tecnico_externo'
    ) THEN
        ALTER TABLE prueba_calidad ADD COLUMN tecnico_externo VARCHAR(150);
    END IF;

    -- -----------------------------------------------------------------
    -- Resumen final
    -- -----------------------------------------------------------------
    SELECT 'Migración completada. Las columnas nuevas ya existen o fueron añadidas.' AS mensaje;
END$$
DELIMITER ;

CALL sp_migrar_base_existente();
DROP PROCEDURE sp_migrar_base_existente;
