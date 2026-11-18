-- Ejecutar manualmente en la base existente; conserva el historial de pruebas.
-- Seleccionar previamente la base configurada en SPRING_DATASOURCE_URL.
DROP PROCEDURE IF EXISTS migrate_pruebas_calidad_logical_delete;
DELIMITER $$
CREATE PROCEDURE migrate_pruebas_calidad_logical_delete()
BEGIN
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
END$$
DELIMITER ;
CALL migrate_pruebas_calidad_logical_delete();
DROP PROCEDURE migrate_pruebas_calidad_logical_delete;
