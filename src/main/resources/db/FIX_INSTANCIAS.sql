-- =====================================================================
-- Smart Power On S.A.C. - Fix: catálogo sin serie + tabla de instancias
-- =====================================================================
-- Ejecutar UNA VEZ sobre la base MySQL existente para alinear el esquema
-- con el informe (catálogo de modelos con código/modelo/marca; número de
-- serie por unidad en material_instancia).
--
-- Qué hace:
--   1. Elimina las columnas legacy numero_serie / numero_modelo de material
--      (la serie ya no es atributo del catálogo, sino de cada unidad).
--   2. Crea la tabla material_instancia (una fila por unidad, con
--      numero_serie único) para la trazabilidad individual.
--
-- Como usar (MySQL Workbench o consola):
--   USE smart_power_on_db;
--   source db/FIX_INSTANCIAS.sql;
-- =====================================================================

USE smart_power_on_db;

-- 1. Eliminar columnas legacy de numero de serie/modelo del catálogo
DROP PROCEDURE IF EXISTS sp_fix_instancias;
DELIMITER $$
CREATE PROCEDURE sp_fix_instancias()
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'numero_serie') THEN
        ALTER TABLE material DROP COLUMN numero_serie;
    END IF;
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'numero_modelo') THEN
        ALTER TABLE material DROP COLUMN numero_modelo;
    END IF;
END$$
DELIMITER ;
CALL sp_fix_instancias();
DROP PROCEDURE sp_fix_instancias;

-- 2. Crear tabla de instancias (una fila por unidad con número de serie único)
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

-- Verificación: material NO debe tener numero_serie/numero_modelo;
-- material_instancia debe existir con la columna numero_serie.
SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND ((TABLE_NAME = 'material' AND COLUMN_NAME IN ('numero_serie','numero_modelo'))
        OR (TABLE_NAME = 'material_instancia' AND COLUMN_NAME = 'numero_serie'));
