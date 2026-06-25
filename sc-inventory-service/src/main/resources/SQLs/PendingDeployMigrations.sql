-- ============================================================
-- PENDING DEPLOY MIGRATIONS
--
-- There is no Flyway/Liquibase in this project (ddl-auto=none), so
-- schema changes are applied by hand. Convention:
--   1. Every schema-changing code change gets its SQL appended below
--      (with a comment header explaining what/why), instead of a new
--      one-off file.
--   2. After deploying the code, run everything in this file by hand
--      against every environment that needs it (run once per tenant
--      schema for tenant-schema changes; once for masterschema changes).
--   3. Once confirmed applied everywhere, empty this file back out
--      (keep this header block) so it always reflects only what is
--      still outstanding.
-- ============================================================

-- ----------------------------------------------------------------
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
-- ----------------------------------------------------------------

ALTER TABLE drgtrdcntr.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE bhaavbhumi.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE citycenter.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE mnglmcity.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE mhvrtrdcntr.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE iseries.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE smartcity.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE dextension.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE bextension.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;

ALTER TABLE anantamsamosharan.outward_inventory
    DROP FOREIGN KEY FKk7s86tr3i8o8m9mx35786fskp,
    DROP COLUMN warehouse_id;