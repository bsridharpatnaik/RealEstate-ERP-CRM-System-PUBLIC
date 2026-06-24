-- ============================================================
-- Drop legacy header-level warehouse from outward_inventory
-- Run once per tenant schema (warehouse now lives on each line
-- in InwardOutwardList instead — this header column has had no
-- remaining readers since outward was made multi-warehouse-aware).
--
-- Confirmed via local dev DB: FK name below
-- (FKk7s86tr3i8o8m9mx35786fskp) is identical across every existing
-- tenant schema (Hibernate auto-generates it deterministically from
-- table/column names), so it should match in every environment.
-- If a DROP FOREIGN KEY statement below errors with "check that
-- column/key exists", look up the real name first with:
--   SELECT constraint_name FROM information_schema.key_column_usage
--   WHERE table_schema = '<schema>' AND table_name = 'outward_inventory'
--     AND column_name = 'warehouse_id' AND referenced_table_name IS NOT NULL;
-- ============================================================

ALTER TABLE outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp;

ALTER TABLE outward_inventory
    DROP COLUMN warehouse_id;
