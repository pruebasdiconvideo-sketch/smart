-- Ejecutar manualmente en la base existente. Omite las columnas que ya existan.
-- Seleccionar antes la base de datos configurada en SPRING_DATASOURCE_URL.
DROP PROCEDURE IF EXISTS migrate_material_extra_columns;
DELIMITER $$
CREATE PROCEDURE migrate_material_extra_columns()
BEGIN
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
    -- en material_instancia). Eliminar columnas legacy si existen.
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
END$$
DELIMITER ;
CALL migrate_material_extra_columns();
DROP PROCEDURE migrate_material_extra_columns;

-- Instancia física de un material (una unidad) con número de serie único.
-- Trazabilidad individual por unidad (informe). Stock consolidado en material.stock_actual.
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
