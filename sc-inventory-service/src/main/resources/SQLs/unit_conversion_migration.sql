-- Run against the MASTER schema
-- Alternate billing units per product
CREATE TABLE IF NOT EXISTS product_unit_conversions (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT       NOT NULL,
    unit_name          VARCHAR(100) NOT NULL,
    conversion_factor  DOUBLE       NOT NULL COMMENT '1 billing unit = conversion_factor base units',
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_puc_product FOREIGN KEY (product_id) REFERENCES Product(productId)
);

-- PO line billing unit capture
ALTER TABLE purchase_order_line
    ADD COLUMN IF NOT EXISTS billing_unit              VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS billing_quantity          DOUBLE       NULL,
    ADD COLUMN IF NOT EXISTS billing_conversion_factor DOUBLE       NULL;
