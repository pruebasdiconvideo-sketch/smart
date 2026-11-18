-- Ejecutar manualmente en la base existente; conserva solicitudes y despachos.
-- Seleccionar previamente la base configurada en SPRING_DATASOURCE_URL.
DROP PROCEDURE IF EXISTS migrate_operaciones_logical_delete;
DELIMITER $$
CREATE PROCEDURE migrate_operaciones_logical_delete()
BEGIN
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
END$$
DELIMITER ;
CALL migrate_operaciones_logical_delete();
DROP PROCEDURE migrate_operaciones_logical_delete;
