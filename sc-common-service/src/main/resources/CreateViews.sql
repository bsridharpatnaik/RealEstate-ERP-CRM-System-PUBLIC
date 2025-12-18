use common;

DELIMITER $$

CREATE PROCEDURE cleanup_old_api_logs()
BEGIN
    DECLARE rows_deleted INT DEFAULT 0;
    DECLARE total_deleted INT DEFAULT 0;

    -- Delete in batches to avoid long locks
    REPEAT
        DELETE FROM api_log
        WHERE timestamp < DATE_SUB(NOW(), INTERVAL 20 DAY)
        LIMIT 10000;

        SET rows_deleted = ROW_COUNT();
        SET total_deleted = total_deleted + rows_deleted;

        -- Sleep briefly between batches to reduce load
        DO SLEEP(1);

    UNTIL rows_deleted = 0 END REPEAT;

    -- Optional: Log the cleanup (if you have a logging table)
    -- INSERT INTO cleanup_log (table_name, records_deleted, cleanup_date)
    -- VALUES ('api_log', total_deleted, NOW());

END$$

DELIMITER ;

-- Make sure event scheduler is enabled
SET GLOBAL event_scheduler = ON;

-- Create the event to run daily at 2 AM
CREATE EVENT IF NOT EXISTS daily_api_log_cleanup
ON SCHEDULE EVERY 1 DAY
STARTS (TIMESTAMP(CURRENT_DATE) + INTERVAL 1 DAY + INTERVAL 2 HOUR)
DO
  CALL cleanup_old_api_logs();