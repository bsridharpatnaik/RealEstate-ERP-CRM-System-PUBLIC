-- Migration: Remove header-level need_by_date from indent_inventory
-- Expected delivery date is now stored only at line-item level (indent_inventory_entries.need_by_date)
-- The enricher computes the header-level date as min(line_item.need_by_date) at query time.
--
-- Run this against every tenant schema.

ALTER TABLE suncitynxv2.indent_inventory    DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE kalpavrishv2.indent_inventory   DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE riddhisiddhiv2.indent_inventory DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE smartcityv2.indent_inventory    DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE businessparkv2.indent_inventory DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE drgtrdcntrv2.indent_inventory   DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE citycenterv2.indent_inventory   DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE schoolv2.indent_inventory       DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE bhaavbhumiv2.indent_inventory   DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE dhabbav2.indent_inventory       DROP COLUMN IF EXISTS need_by_date;
ALTER TABLE mhvrtrdcntrv2.indent_inventory  DROP COLUMN IF EXISTS need_by_date;
