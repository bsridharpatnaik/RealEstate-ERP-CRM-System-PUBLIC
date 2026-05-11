package com.ec.application.service;

import com.ec.application.data.ProductMergePreviewDTO;
import com.ec.application.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Executes all per-tenant and master-schema SQL for product merge inside REQUIRES_NEW
 * transactions, so AbstractRoutingDataSource picks up the ThreadLocal schema set by the caller.
 */
@Component
@RequiredArgsConstructor
public class ProductMergeTenantExecutor {

    private final JdbcTemplate jdbcTemplate;

    // ── Preview ───────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductMergePreviewDTO.TenantUsageSummary countUsages(String tenantSchema, Long sourceId) {
        ProductMergePreviewDTO.TenantUsageSummary s = new ProductMergePreviewDTO.TenantUsageSummary();
        s.setTenantSchema(tenantSchema);
        s.setStockEntries(count("SELECT COUNT(*) FROM stock WHERE productId=? AND is_deleted=false", sourceId));
        s.setStockHistoryEntries(count("SELECT COUNT(*) FROM stock_history WHERE productId=?", sourceId));
        s.setInwardOutwardEntries(count("SELECT COUNT(*) FROM inward_outward_entries WHERE productId=?", sourceId));
        s.setRejectInwardEntries(count("SELECT COUNT(*) FROM reject_inward_entries WHERE productId=?", sourceId));
        s.setRejectOutwardEntries(count("SELECT COUNT(*) FROM reject_outward_entries WHERE productId=?", sourceId));
        s.setReturnOutwardEntries(count("SELECT COUNT(*) FROM return_outward_entries WHERE productId=?", sourceId));
        // indent and PO counts are in master schema — see countMasterUsages
        s.setTransferItemEntries(count("SELECT COUNT(*) FROM inventory_transfer_item WHERE productId=?", sourceId));
        s.setLostDamagedEntries(count("SELECT COUNT(*) FROM lost_damaged_inventory WHERE productId=?", sourceId));
        s.setBoqUploadEntries(count("SELECT COUNT(*) FROM BOQUpload WHERE productId=? AND is_deleted=false", sourceId));
        s.setBoqInventoryEntries(count("SELECT COUNT(*) FROM boq_inventory WHERE productId=? AND is_deleted=false", sourceId));
        s.setBoqHistoryEntries(count("SELECT COUNT(*) FROM boq_history WHERE productId=?", sourceId));
        s.setPricingEntries(count("SELECT COUNT(*) FROM InventoryMonthPriceMapping WHERE productId=? AND is_deleted=false", sourceId));
        return s;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductMergePreviewDTO.GlobalUsageSummary countMasterUsages(Long sourceId) {
        ProductMergePreviewDTO.GlobalUsageSummary s = new ProductMergePreviewDTO.GlobalUsageSummary();
        s.setIndentEntries(count("SELECT COUNT(*) FROM indent_inventory_entries WHERE productId=?", sourceId));
        s.setPurchaseOrderLineEntries(count("SELECT COUNT(*) FROM purchase_order_line WHERE product_id=?", sourceId));
        return s;
    }

    // ── Per-tenant merge ──────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeTenantMerge(Long sourceId, Long targetId, Product targetProduct) {

        // 1. Stock: merge per warehouse
        mergeStock(sourceId, targetId);

        // 2. Simple productId-only reassignments
        jdbcTemplate.update("UPDATE inward_outward_entries SET productId=? WHERE productId=?", targetId, sourceId);
        jdbcTemplate.update("UPDATE reject_inward_entries SET productId=? WHERE productId=?", targetId, sourceId);
        jdbcTemplate.update("UPDATE reject_outward_entries SET productId=? WHERE productId=?", targetId, sourceId);
        jdbcTemplate.update("UPDATE return_outward_entries SET productId=? WHERE productId=?", targetId, sourceId);
        jdbcTemplate.update("UPDATE lost_damaged_inventory SET productId=? WHERE productId=?", targetId, sourceId);
        jdbcTemplate.update("UPDATE boq_history SET productId=? WHERE productId=?", targetId, sourceId);

        // 3. Reassign + update measurementUnit in stock_history
        jdbcTemplate.update(
            "UPDATE stock_history SET productId=?, measurementUnit=? WHERE productId=?",
            targetId, targetProduct.getMeasurementUnit(), sourceId);

        // indent_inventory_entries and purchase_order_line live in master schema — updated in executeMasterMerge

        // 5. Reassign + update all denormalized fields in inventory_transfer_item
        jdbcTemplate.update(
            "UPDATE inventory_transfer_item SET productId=?, productName=?, productCode=?, measurementUnit=? WHERE productId=?",
            targetId, targetProduct.getProductName(), targetProduct.getProductCode(),
            targetProduct.getMeasurementUnit(), sourceId);

        // 6. BOQUpload: soft-delete conflicting rows, then reassign remaining
        mergeBOQUpload(sourceId, targetId);

        // 7. boq_inventory: same conflict logic
        mergeBOQInventory(sourceId, targetId);

        // 8. InventoryMonthPriceMapping: discard source months that already exist on target
        mergePricing(sourceId, targetId);

        // 9. product_tenant_config: soft-delete source config
        jdbcTemplate.update(
            "DELETE FROM product_tenant_config WHERE product_id=?", sourceId);
    }

    // ── Master-schema merge ───────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeMasterMerge(Long sourceId, Long targetId, Product targetProduct) {
        mergeStockSummary(sourceId, targetId, targetProduct);

        // Indents and POs live in master schema
        jdbcTemplate.update(
            "UPDATE indent_inventory_entries SET productId=?, measurement_unit=? WHERE productId=?",
            targetId, targetProduct.getMeasurementUnit(), sourceId);
        jdbcTemplate.update(
            "UPDATE purchase_order_line SET product_id=? WHERE product_id=?",
            targetId, sourceId);

        jdbcTemplate.update("UPDATE Product SET is_deleted=true WHERE productId=?", sourceId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void mergeStock(Long sourceId, Long targetId) {
        List<Map<String, Object>> sourceRows = jdbcTemplate.queryForList(
            "SELECT stockId, warehouseId, quantityInHand FROM stock WHERE productId=? AND is_deleted=false",
            sourceId);

        for (Map<String, Object> row : sourceRows) {
            long stockId = toLong(row.get("stockId"));
            long warehouseId = toLong(row.get("warehouseId"));
            double sourceQty = toDouble(row.get("quantityInHand"));

            List<Map<String, Object>> targetRows = jdbcTemplate.queryForList(
                "SELECT stockId, quantityInHand FROM stock WHERE productId=? AND warehouseId=? AND is_deleted=false",
                targetId, warehouseId);

            if (!targetRows.isEmpty()) {
                long targetStockId = toLong(targetRows.get(0).get("stockId"));
                double targetQty = toDouble(targetRows.get(0).get("quantityInHand"));
                jdbcTemplate.update("UPDATE stock SET quantityInHand=? WHERE stockId=?",
                    sourceQty + targetQty, targetStockId);
                jdbcTemplate.update("UPDATE stock SET is_deleted=true WHERE stockId=?", stockId);
            } else {
                jdbcTemplate.update("UPDATE stock SET productId=? WHERE stockId=?", targetId, stockId);
            }
        }
    }

    private void mergeBOQUpload(Long sourceId, Long targetId) {
        jdbcTemplate.update(
            "UPDATE BOQUpload b1" +
            "  INNER JOIN BOQUpload b2" +
            "    ON b2.productId=? AND b2.buildingTypeId=b1.buildingTypeId" +
            "    AND b2.usageLocationId=b1.usageLocationId" +
            "    AND COALESCE(b2.locationId,0)=COALESCE(b1.locationId,0)" +
            "    AND b2.is_deleted=false" +
            " SET b1.is_deleted=true" +
            " WHERE b1.productId=? AND b1.is_deleted=false",
            targetId, sourceId);

        jdbcTemplate.update(
            "UPDATE BOQUpload SET productId=? WHERE productId=? AND is_deleted=false",
            targetId, sourceId);
    }

    private void mergeBOQInventory(Long sourceId, Long targetId) {
        jdbcTemplate.update(
            "UPDATE boq_inventory b1" +
            "  INNER JOIN boq_inventory b2" +
            "    ON b2.productId=? AND b2.typeId=b1.typeId" +
            "    AND COALESCE(b2.locationId,0)=COALESCE(b1.locationId,0)" +
            "    AND b2.is_deleted=false" +
            " SET b1.is_deleted=true" +
            " WHERE b1.productId=? AND b1.is_deleted=false",
            targetId, sourceId);

        jdbcTemplate.update(
            "UPDATE boq_inventory SET productId=? WHERE productId=? AND is_deleted=false",
            targetId, sourceId);
    }

    private void mergePricing(Long sourceId, Long targetId) {
        jdbcTemplate.update(
            "UPDATE InventoryMonthPriceMapping m1" +
            "  INNER JOIN InventoryMonthPriceMapping m2" +
            "    ON m2.productId=? AND DATE_FORMAT(m2.date,'%Y-%m')=DATE_FORMAT(m1.date,'%Y-%m')" +
            "    AND m2.is_deleted=false" +
            " SET m1.is_deleted=true" +
            " WHERE m1.productId=? AND m1.is_deleted=false",
            targetId, sourceId);

        jdbcTemplate.update(
            "UPDATE InventoryMonthPriceMapping SET productId=? WHERE productId=? AND is_deleted=false",
            targetId, sourceId);
    }

    private void mergeStockSummary(Long sourceId, Long targetId, Product targetProduct) {
        List<Map<String, Object>> sourceRows = jdbcTemplate.queryForList(
            "SELECT id, tenantSchema, warehouseId, quantityInHand FROM stock_summary WHERE productId=? AND is_deleted=false",
            sourceId);

        for (Map<String, Object> row : sourceRows) {
            long summaryId = toLong(row.get("id"));
            String tenantSchema = (String) row.get("tenantSchema");
            long warehouseId = toLong(row.get("warehouseId"));
            double sourceQty = toDouble(row.get("quantityInHand"));

            List<Map<String, Object>> targetRows = jdbcTemplate.queryForList(
                "SELECT id, quantityInHand FROM stock_summary WHERE productId=? AND tenantSchema=? AND warehouseId=? AND is_deleted=false",
                targetId, tenantSchema, warehouseId);

            if (!targetRows.isEmpty()) {
                long targetSummaryId = toLong(targetRows.get(0).get("id"));
                double targetQty = toDouble(targetRows.get(0).get("quantityInHand"));
                jdbcTemplate.update(
                    "UPDATE stock_summary SET quantityInHand=?, productName=?, productCode=?, measurement_unit=? WHERE id=?",
                    sourceQty + targetQty,
                    targetProduct.getProductName(), targetProduct.getProductCode(),
                    targetProduct.getMeasurementUnit(), targetSummaryId);
                jdbcTemplate.update("UPDATE stock_summary SET is_deleted=true WHERE id=?", summaryId);
            } else {
                jdbcTemplate.update(
                    "UPDATE stock_summary SET productId=?, productName=?, productCode=?, measurement_unit=? WHERE id=?",
                    targetId, targetProduct.getProductName(), targetProduct.getProductCode(),
                    targetProduct.getMeasurementUnit(), summaryId);
            }
        }
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result != null ? result : 0L;
    }

    private long toLong(Object val) {
        return ((Number) val).longValue();
    }

    private double toDouble(Object val) {
        return ((Number) val).doubleValue();
    }
}
