-- Ejecutar manualmente en la base existente; no elimina datos.
-- Seleccionar previamente la base configurada en SPRING_DATASOURCE_URL.
DROP PROCEDURE IF EXISTS migrate_parametro_prueba_activo;
DELIMITER $$
CREATE PROCEDURE migrate_parametro_prueba_activo()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'parametro_prueba'
          AND COLUMN_NAME = 'activo'
    ) THEN
        ALTER TABLE parametro_prueba
            ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END$$
DELIMITER ;
CALL migrate_parametro_prueba_activo();
DROP PROCEDURE migrate_parametro_prueba_activo;
