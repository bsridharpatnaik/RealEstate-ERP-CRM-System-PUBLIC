-- Quote Comparison feature — schema migrations applied directly to local dev `masterschema`
-- during this session. There is no Flyway/Liquibase in this project (ddl-auto=none), so these
-- must be re-run by hand against masterschema in every other environment (staging/prod) before
-- deploying this code. All are additive/widening — safe to run on a live table.

USE masterschema;

-- 1. Supplier quote revisions (R-0, R-1, R-2...) — lets the same vendor be quoted multiple
--    times in one comparison.
ALTER TABLE supplier_quote
  ADD COLUMN revision_label VARCHAR(50) NULL DEFAULT 'R-0';

-- 2. Per-criterion scope — LINE (varies per product) vs HEADER (one value per vendor).
ALTER TABLE comparison_criteria
  ADD COLUMN criteria_scope VARCHAR(20) NOT NULL DEFAULT 'LINE';

-- 3. Header-scoped criteria values need a value row that isn't tied to a specific line.
ALTER TABLE supplier_quote_criteria_value
  MODIFY COLUMN supplier_quote_line_id BIGINT NULL,
  ADD COLUMN supplier_quote_id BIGINT NULL;

-- 4. Indent line "quote requested" marker — independent of line_item_status (which is owned by
--    the PO/inward pipeline and gets recomputed on every inward sync).
ALTER TABLE indent_inventory_entries
  ADD COLUMN quote_requested_qc_id VARCHAR(20) NULL;
