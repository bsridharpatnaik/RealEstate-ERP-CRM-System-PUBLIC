-- ============================================================
-- Batch Tracking Feature Migration
-- Run once per tenant schema + master schema for Product table
-- ============================================================

-- 1. Add isExpirable flag to Product
ALTER TABLE Product
    ADD COLUMN IF NOT EXISTS is_expirable BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add brand and expiry_date to inward_outward_entries (carries batch info through inward flow)
ALTER TABLE inward_outward_entries
    ADD COLUMN IF NOT EXISTS brand VARCHAR(255),
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

-- 3. Add hasFifoOverride flag to outward_inventory
ALTER TABLE outward_inventory
    ADD COLUMN IF NOT EXISTS has_fifo_override BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Create inventory_batch table
CREATE TABLE IF NOT EXISTS inventory_batch (
    batch_id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT NOT NULL,
    warehouse_id     BIGINT NOT NULL,
    inward_id        BIGINT NOT NULL,
    brand            VARCHAR(255),
    expiry_date      DATE,
    received_date    DATE NOT NULL,
    qty_received     DOUBLE NOT NULL,
    qty_remaining    DOUBLE NOT NULL,
    alert_sent_60    BOOLEAN NOT NULL DEFAULT FALSE,
    alert_sent_30    BOOLEAN NOT NULL DEFAULT FALSE,
    alert_sent_expired BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by       VARCHAR(255),
    creation_date    DATETIME,
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME
);

-- 5. Create outward_batch_consumption table
CREATE TABLE IF NOT EXISTS outward_batch_consumption (
    id               BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    outward_id       BIGINT NOT NULL,
    batch_id         BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    warehouse_id     BIGINT NOT NULL,
    qty_consumed     DOUBLE NOT NULL,
    fifo_overridden  BOOLEAN NOT NULL DEFAULT FALSE,
    override_comment VARCHAR(500),
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by       VARCHAR(255),
    creation_date    DATETIME,
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME
);

-- 6. Update stockInformation view to include warehouseId in detailedStock JSON
-- (required for the batch tracking Batches tab warehouse selector)
CREATE OR REPLACE VIEW stockInformation as
    SELECT
        p.productId as productId,
        p.product_name,
        p.product_code,
        p.reorderQuantity,
        p.measurementUnit,
        c.category_name,
        ROUND(SUM(s.quantityInHand),2) as totalQuantityInHand,
        CASE WHEN ROUND(SUM(s.quantityInHand),2)<=p.reorderQuantity THEN 'Low' ELSE 'High' END as stockStatus,
        JSON_ARRAYAGG(JSON_OBJECT(
            'warehouseId', w.warehouse_id,
            'warehouseName', w.warehouseName,
            'quantityInHand', s.quantityInHand,
            'measurementUnit', p.measurementUnit
            )) as detailedStock
    FROM Stock s
    INNER JOIN Product p on p.productId=s.productId
    INNER JOIN Category c on p.categoryId=c.categoryId
    INNER JOIN Warehouse w on w.warehouse_id = s.warehouseId
    WHERE s.is_deleted=0
    GROUP BY p.productId,p.product_name,p.product_code,p.reorderQuantity,p.measurementUnit,c.category_name;

-- 7. Create batch_write_off table
CREATE TABLE IF NOT EXISTS batch_write_off (
    write_off_id     BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id         BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    warehouse_id     BIGINT NOT NULL,
    quantity         DOUBLE NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    write_off_date   DATE NOT NULL,
    written_off_by   VARCHAR(255),
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by       VARCHAR(255),
    creation_date    DATETIME,
    last_modified_by VARCHAR(255),
    last_modified_date DATETIME
);
