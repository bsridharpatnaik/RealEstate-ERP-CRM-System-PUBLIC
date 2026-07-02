-- ============================================================
-- PENDING DEPLOY MIGRATIONS
--
-- There is no Flyway/Liquibase in this project. Additive schema changes
-- (new column/table) auto-apply on backend startup — AutoDDLConfig.java
-- hardcodes hibernate.hbm2ddl.auto=update per tenant schema, regardless
-- of the ddl-auto=none in application.properties. This file is only for
-- what `update` mode can't/won't do: dropped/renamed columns, FK changes,
-- and one-off data corrections. Convention:
--   1. Every non-additive schema change or data correction gets its SQL
--      appended below (with a comment header explaining what/why),
--      instead of a new one-off file.
--   2. After deploying the code, run everything in this file by hand
--      against every environment that needs it (run once per tenant
--      schema for tenant-schema changes; once for masterschema changes).
--   3. Once confirmed applied everywhere, empty this file back out
--      (keep this header block) so it always reflects only what is
--      still outstanding.
--
-- When a block defines a PROCEDURE, select a default DB first (any DB
-- works, e.g. masterschema — the procedure body fully-qualifies target
-- schemas in its dynamic SQL), or `CREATE PROCEDURE` errors with
-- "No database selected":
--   mysql -h <host> -u <user> -p masterschema < PendingDeployMigrations.sql
-- ============================================================

-- ----------------------------------------------------------------
-- Drop legacy header-level warehouse from outward_inventory
-- Run once per tenant schema (warehouse now lives on each line
-- in InwardOutwardList instead — this header column has had no
-- remaining readers since outward was made multi-warehouse-aware).
--
-- The originally assumed FK name (FKk7s86tr3i8o8m9mx35786fskp) does
-- NOT exist on any schema checked locally — there is no FK on this
-- column to drop in practice. Column-already-dropped is also a real
-- state (some schemas were fixed by hand already). The procedure
-- below is idempotent: it looks up the FK by query instead of
-- assuming a name, and no-ops entirely if the column is already gone,
-- so it is safe to run against every environment/schema regardless
-- of what state that schema is already in.
-- ----------------------------------------------------------------

DROP PROCEDURE IF EXISTS drop_outward_warehouse_id_if_exists;

DELIMITER //

CREATE PROCEDURE drop_outward_warehouse_id_if_exists(IN target_schema VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    DECLARE fk_name VARCHAR(128) DEFAULT NULL;

    SELECT COUNT(*) INTO col_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = target_schema
      AND TABLE_NAME = 'outward_inventory'
      AND COLUMN_NAME = 'warehouse_id';

    IF col_exists > 0 THEN
        SELECT constraint_name INTO fk_name
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = target_schema
          AND TABLE_NAME = 'outward_inventory'
          AND COLUMN_NAME = 'warehouse_id'
          AND REFERENCED_TABLE_NAME IS NOT NULL
        LIMIT 1;

        IF fk_name IS NOT NULL THEN
            SET @sql = CONCAT('ALTER TABLE ', target_schema, '.outward_inventory DROP FOREIGN KEY ', fk_name);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        SET @sql = CONCAT('ALTER TABLE ', target_schema, '.outward_inventory DROP COLUMN warehouse_id');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL drop_outward_warehouse_id_if_exists('drgtrdcntr');
CALL drop_outward_warehouse_id_if_exists('bhaavbhumi');
CALL drop_outward_warehouse_id_if_exists('citycenter');
CALL drop_outward_warehouse_id_if_exists('mnglmcity');
CALL drop_outward_warehouse_id_if_exists('mhvrtrdcntr');
CALL drop_outward_warehouse_id_if_exists('iseries');
CALL drop_outward_warehouse_id_if_exists('smartcity');
CALL drop_outward_warehouse_id_if_exists('masterschema');
CALL drop_outward_warehouse_id_if_exists('dextension');
CALL drop_outward_warehouse_id_if_exists('bextension');
CALL drop_outward_warehouse_id_if_exists('anantamsamosharan');
CALL drop_outward_warehouse_id_if_exists('bbextension');
-- Add any other tenant schema names here before running in a new environment.

DROP PROCEDURE drop_outward_warehouse_id_if_exists;

-- ----------------------------------------------------------------
-- Data correction: outward reject previously decremented
-- inward_outward_entries.quantity (wrong — reject doesn't return
-- stock, so the ledger total it feeds must not shrink). Code fixed
-- to track rejected_quantity separately instead. This backfills any
-- line where rejected_quantity is still 0 despite having reject
-- records, adding the wrongly-subtracted amount back.
--
-- Idempotent — only touches lines not already corrected. Caveat:
-- matches by (outwardid, productId) only, not warehouse — RejectOutwardList
-- has no warehouse column. Misattributes only if the same product was
-- outwarded from two different warehouses on the same outward AND
-- rejected on both lines (rare; verify manually if suspected).
-- ----------------------------------------------------------------

DROP PROCEDURE IF EXISTS fix_reject_quantity_drift;

DELIMITER //

CREATE PROCEDURE fix_reject_quantity_drift(IN target_schema VARCHAR(64))
BEGIN
    SET @sql = CONCAT(
        'UPDATE ', target_schema, '.inward_outward_entries ioe ',
        'JOIN ', target_schema, '.outwardinventory_entry oie ON oie.entryid = ioe.entryid ',
        'JOIN ( ',
        '  SELECT roe.outwardid AS outwardid, rie.productId AS productId, ',
        '         SUM(rie.rejectQuantity) AS totalRejected ',
        '  FROM ', target_schema, '.reject_outward_entries rie ',
        '  JOIN ', target_schema, '.rejectOutward_entry roe ON roe.rejectentryid = rie.rejectentryid ',
        '  WHERE rie.is_deleted = 0 ',
        '  GROUP BY roe.outwardid, rie.productId ',
        ') rej ON rej.outwardid = oie.outwardid AND rej.productId = ioe.productId ',
        'SET ioe.quantity = ioe.quantity + rej.totalRejected, ',
        '    ioe.rejected_quantity = rej.totalRejected ',
        'WHERE COALESCE(ioe.rejected_quantity, 0) = 0'
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //

DELIMITER ;

CALL fix_reject_quantity_drift('drgtrdcntr');
CALL fix_reject_quantity_drift('bhaavbhumi');
CALL fix_reject_quantity_drift('businesspark');
CALL fix_reject_quantity_drift('citycenter');
CALL fix_reject_quantity_drift('dextension');
CALL fix_reject_quantity_drift('dhabba');
CALL fix_reject_quantity_drift('iseries');
CALL fix_reject_quantity_drift('kalpavrish');
CALL fix_reject_quantity_drift('mhvrtrdcntr');
CALL fix_reject_quantity_drift('mnglmcity');
CALL fix_reject_quantity_drift('riddhisiddhi');
CALL fix_reject_quantity_drift('school');
CALL fix_reject_quantity_drift('smartcity');
CALL fix_reject_quantity_drift('suncitynx');
CALL fix_reject_quantity_drift('bextension');
CALL fix_reject_quantity_drift('anantamsamosharan');
-- Add any other tenant schema names here before running in a new environment.

DROP PROCEDURE fix_reject_quantity_drift;

-- ----------------------------------------------------------------
-- Data correction: cancel line items for cancelled/rejected indents
-- Indent header cancel/reject previously left child line items in
-- their old status (typically NEW). Code now cascades CANCELLED to
-- all lines on header cancel/reject. This backfill corrects existing
-- bad-data rows.
-- Indents live in masterschema only — run once against masterschema.
-- ----------------------------------------------------------------

UPDATE masterschema.indent_inventory_entries iie
JOIN masterschema.indent_inventory ii ON ii.indent_id = iie.indent_id
SET iie.line_item_status = 'CANCELLED'
WHERE ii.indent_status IN ('CANCELLED', 'REJECTED')
  AND iie.line_item_status != 'CANCELLED'
  AND iie.is_deleted = 0;

-- ----------------------------------------------------------------
-- New role: product-merge-admin
-- Run once against the common-service DB (the one holding the `role`
-- table). Only users explicitly assigned this role will see the
-- Merge Products menu and can call the merge API.
-- ----------------------------------------------------------------

INSERT IGNORE INTO common.role (name) VALUES ('product-merge-admin');