-- Ejecutar manualmente en la base existente; conserva el historial y compensa
-- inventario/trazabilidad al anular registros ya procesados.
-- Seleccionar previamente la base configurada en SPRING_DATASOURCE_URL.
DROP PROCEDURE IF EXISTS migrate_mermas_devoluciones_logical_delete;
DELIMITER $$
CREATE PROCEDURE migrate_mermas_devoluciones_logical_delete()
BEGIN
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
END$$
DELIMITER ;
CALL migrate_mermas_devoluciones_logical_delete();
DROP PROCEDURE migrate_mermas_devoluciones_logical_delete;
