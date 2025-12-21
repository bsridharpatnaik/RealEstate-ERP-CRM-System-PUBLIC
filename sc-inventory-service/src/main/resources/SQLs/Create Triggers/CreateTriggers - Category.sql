use bhaavbhumiv2;
DELIMITER $$

CREATE PROCEDURE regenerate_table_sync_triggers (
    IN p_source_schema   VARCHAR(64),
    IN p_table_name      VARCHAR(64),
    IN p_pk_column       VARCHAR(64),
    IN p_target_schemas  TEXT
)
BEGIN
    DECLARE v_columns TEXT;
    DECLARE v_new_columns TEXT;
    DECLARE v_set_clause TEXT;

    DECLARE v_done INT DEFAULT 0;
    DECLARE v_target_schema VARCHAR(64);

    -- Cursor to iterate target schemas
    DECLARE cur_targets CURSOR FOR
        SELECT TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(p_target_schemas, ',', seq), ',', -1))
        FROM (
            SELECT @row := @row + 1 AS seq
            FROM information_schema.columns, (SELECT @row := 0) r
            LIMIT 100
        ) seqs
        WHERE seq <= 1 + LENGTH(p_target_schemas) - LENGTH(REPLACE(p_target_schemas, ',', ''));

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    /* ----------------------------------------------------
       Fetch column metadata dynamically
    ---------------------------------------------------- */
    SELECT
        GROUP_CONCAT(column_name ORDER BY ordinal_position),
        GROUP_CONCAT(CONCAT('NEW.', column_name) ORDER BY ordinal_position),
        GROUP_CONCAT(CONCAT(column_name, '=NEW.', column_name) ORDER BY ordinal_position)
    INTO
        v_columns,
        v_new_columns,
        v_set_clause
    FROM information_schema.columns
    WHERE table_schema = p_source_schema
      AND table_name   = p_table_name;

    /* ----------------------------------------------------
       Build trigger names (unique per table)
    ---------------------------------------------------- */
    SET @trg_ins = CONCAT('sync_', p_source_schema, '_', p_table_name, '_ai');
    SET @trg_upd = CONCAT('sync_', p_source_schema, '_', p_table_name, '_au');
    SET @trg_del = CONCAT('sync_', p_source_schema, '_', p_table_name, '_ad');

    /* ----------------------------------------------------
       Drop existing triggers safely
    ---------------------------------------------------- */
    SET @sql = CONCAT('DROP TRIGGER IF EXISTS ', p_source_schema, '.', @trg_ins);
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    SET @sql = CONCAT('DROP TRIGGER IF EXISTS ', p_source_schema, '.', @trg_upd);
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    SET @sql = CONCAT('DROP TRIGGER IF EXISTS ', p_source_schema, '.', @trg_del);
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    /* ----------------------------------------------------
       Start building trigger bodies
    ---------------------------------------------------- */
    SET @sql_ins = CONCAT(
        'CREATE TRIGGER ', @trg_ins,
        ' AFTER INSERT ON ', p_source_schema, '.', p_table_name,
        ' FOR EACH ROW BEGIN '
    );

    SET @sql_upd = CONCAT(
        'CREATE TRIGGER ', @trg_upd,
        ' AFTER UPDATE ON ', p_source_schema, '.', p_table_name,
        ' FOR EACH ROW BEGIN '
    );

    SET @sql_del = CONCAT(
        'CREATE TRIGGER ', @trg_del,
        ' AFTER DELETE ON ', p_source_schema, '.', p_table_name,
        ' FOR EACH ROW BEGIN '
    );

    /* ----------------------------------------------------
       Append logic for each target schema
    ---------------------------------------------------- */
    OPEN cur_targets;

    read_loop: LOOP
        FETCH cur_targets INTO v_target_schema;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        SET @sql_ins = CONCAT(
            @sql_ins,
            'INSERT INTO ', v_target_schema, '.', p_table_name,
            ' (', v_columns, ') VALUES (', v_new_columns, '); '
        );

        SET @sql_upd = CONCAT(
            @sql_upd,
            'UPDATE ', v_target_schema, '.', p_table_name,
            ' SET ', v_set_clause,
            ' WHERE ', p_pk_column, ' = NEW.', p_pk_column, '; '
        );

        SET @sql_del = CONCAT(
            @sql_del,
            'DELETE FROM ', v_target_schema, '.', p_table_name,
            ' WHERE ', p_pk_column, ' = OLD.', p_pk_column, '; '
        );
    END LOOP;

    CLOSE cur_targets;

    /* ----------------------------------------------------
       Finalize & create triggers
    ---------------------------------------------------- */
    SET @sql_ins = CONCAT(@sql_ins, ' END');
    SET @sql_upd = CONCAT(@sql_upd, ' END');
    SET @sql_del = CONCAT(@sql_del, ' END');

    PREPARE stmt FROM @sql_ins;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    PREPARE stmt FROM @sql_upd;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    PREPARE stmt FROM @sql_del;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

END$$

DELIMITER ;
