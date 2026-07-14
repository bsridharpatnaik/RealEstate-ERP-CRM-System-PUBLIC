package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.ProductMergePreviewDTO;
import com.ec.application.data.ProductMergeResultDTO;
import com.ec.application.model.Product;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductMergeService {

    private static final Logger log = LoggerFactory.getLogger(ProductMergeService.class);

    private final ProductRepo productRepo;
    private final SchemaConfig schemaConfig;
    private final ProductMergeTenantExecutor tenantExecutor;
    private final StockSyncOrchestrator stockSyncOrchestrator;
    private final AllInventoryService allInventoryService;

    // ── Preview ───────────────────────────────────────────────────────────────

    @UseDefaultTenant
    public ProductMergePreviewDTO preview(Long sourceId, Long targetId) {
        validate(sourceId, targetId);

        Product source = getProduct(sourceId);
        Product target = getProduct(targetId);

        validateBatchModeCompatibility(source, target);

        ProductMergePreviewDTO dto = new ProductMergePreviewDTO();
        dto.setSourceProduct(toInfo(source));
        dto.setTargetProduct(toInfo(target));

        List<ProductMergePreviewDTO.TenantUsageSummary> summaries = new ArrayList<>();
        String originalTenant = ThreadLocalStorage.getTenantName();
        try {
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                ThreadLocalStorage.setTenantName(tenant);
                summaries.add(tenantExecutor.countUsages(tenant, sourceId));
            }
            // Indents and POs live in master schema — count separately
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            dto.setGlobalSummary(tenantExecutor.countMasterUsages(sourceId));
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }

        dto.setTenantSummaries(summaries);
        return dto;
    }

    // ── Execute ───────────────────────────────────────────────────────────────

    @UseDefaultTenant
    public ProductMergeResultDTO execute(Long sourceId, Long targetId) {
        validate(sourceId, targetId);

        // Source may be soft-deleted from a previous partial merge attempt — bypass @Where filter
        Product source = getProductIncludingDeleted(sourceId);
        Product target = getProduct(targetId);

        validateBatchModeCompatibility(source, target);

        List<ProductMergeResultDTO.TenantMergeResult> results = new ArrayList<>();
        String originalTenant = ThreadLocalStorage.getTenantName();
        boolean allSuccess = true;

        try {
            // Per-tenant merges
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                ThreadLocalStorage.setTenantName(tenant);
                try {
                    tenantExecutor.executeTenantMerge(sourceId, targetId, target);
                    results.add(new ProductMergeResultDTO.TenantMergeResult(tenant, true, null));
                    log.info("Product merge completed for tenant {}", tenant);
                } catch (Exception e) {
                    allSuccess = false;
                    results.add(new ProductMergeResultDTO.TenantMergeResult(tenant, false, e.getMessage()));
                    log.error("Product merge failed for tenant {}: {}", tenant, e.getMessage(), e);
                }
            }

            // Master-schema: stock_summary + soft-delete source product
            // Only runs when ALL tenants succeeded — keeps source product alive for retry on partial failure
            if (allSuccess) {
                ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
                try {
                    tenantExecutor.executeMasterMerge(sourceId, targetId, target);
                    results.add(new ProductMergeResultDTO.TenantMergeResult("master", true, null));
                    log.info("Product merge master schema completed");
                } catch (Exception e) {
                    allSuccess = false;
                    results.add(new ProductMergeResultDTO.TenantMergeResult("master", false, e.getMessage()));
                    log.error("Product merge failed for master schema: {}", e.getMessage(), e);
                }
            }

        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }

        // Trigger stock sync to recalculate global summaries
        try {
            stockSyncOrchestrator.syncAllTenants();
        } catch (Exception e) {
            log.warn("Stock sync after merge failed: {}", e.getMessage());
        }

        // Rebuild the all_inventory rollup per tenant — proc runs in the current
        // ThreadLocal schema, so switch schema before each call. Otherwise the
        // dashboard keeps showing the soft-deleted source product until its next
        // scheduled proc run. Other report syncs (aging/dead/expired/low-stock)
        // self-heal on their own schedule.
        try {
            for (String tenant : schemaConfig.getNonMasterSchemaList()) {
                try {
                    ThreadLocalStorage.setTenantName(tenant);
                    allInventoryService.updateAllInventoryTable();
                } catch (Exception e) {
                    log.warn("all_inventory refresh after merge failed for tenant {}: {}", tenant, e.getMessage());
                }
            }
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }

        ProductMergeResultDTO result = new ProductMergeResultDTO();
        result.setOverallSuccess(allSuccess);
        result.setTenantResults(results);
        result.setMessage(allSuccess
            ? "Merge completed successfully across all sites."
            : "Merge completed with some failures. Retry to process failed sites.");
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateBatchModeCompatibility(Product source, Product target) {
        com.ec.application.constants.BatchMode sourceMode = source.getBatchMode() != null
                ? source.getBatchMode() : com.ec.application.constants.BatchMode.NONE;
        com.ec.application.constants.BatchMode targetMode = target.getBatchMode() != null
                ? target.getBatchMode() : com.ec.application.constants.BatchMode.NONE;
        if (sourceMode != targetMode) {
            throw new IllegalArgumentException(
                    "Cannot merge products with different batch tracking modes. "
                    + "'" + source.getProductName() + "' is " + sourceMode
                    + " but '" + target.getProductName() + "' is " + targetMode
                    + ". Both products must have the same batch mode before merging.");
        }
    }

    private void validate(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null) {
            throw new IllegalArgumentException("Source and target product IDs are required.");
        }
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Source and target product cannot be the same.");
        }
    }

    private Product getProduct(Long productId) {
        return productRepo.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private Product getProductIncludingDeleted(Long productId) {
        return productRepo.findByIdIncludingDeleted(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    private ProductMergePreviewDTO.ProductInfo toInfo(Product p) {
        ProductMergePreviewDTO.ProductInfo info = new ProductMergePreviewDTO.ProductInfo();
        info.setProductId(p.getProductId());
        info.setProductName(p.getProductName());
        info.setProductCode(p.getProductCode());
        info.setMeasurementUnit(p.getMeasurementUnit());
        return info;
    }
}
