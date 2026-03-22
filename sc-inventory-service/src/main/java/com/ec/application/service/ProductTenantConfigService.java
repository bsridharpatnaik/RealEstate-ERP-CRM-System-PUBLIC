package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.data.AllTenantReorderConfigDTO;
import com.ec.application.model.Product;
import com.ec.application.model.ProductTenantConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.ProductTenantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.HashMap;

/**
 * Manages tenant-specific reorder level overrides stored in each tenant's
 * product_tenant_config table.
 *
 * IMPORTANT — why @UseDefaultTenant was removed:
 *   @UseDefaultTenant forced ALL queries to the master schema.  Overrides must
 *   live in each tenant's own schema, not the master schema.
 *
 * Schema-switching pattern used here:
 *   1. Caller sets ThreadLocalStorage.setTenantName(tenant) BEFORE calling
 *      TenantSchemaExecutor methods.
 *   2. TenantSchemaExecutor methods use @Transactional(REQUIRES_NEW), which
 *      causes Spring to open a brand-new transaction and acquire a fresh
 *      connection.  AbstractRoutingDataSource.determineCurrentLookupKey()
 *      is called at connection-acquisition time and reads the ThreadLocal that
 *      the caller just set — so the correct schema is used.
 *   3. The original ThreadLocal value is always restored in a finally block.
 */
@Service
@RequiredArgsConstructor
public class ProductTenantConfigService {

    private final ProductTenantConfigRepository configRepo;
    private final SchemaConfig schemaConfig;
    private final TenantSchemaExecutor tenantSchemaExecutor;

    // ── Used by scheduled/notification services (tenant already in ThreadLocal) ─

    /**
     * Returns the effective reorder level for the CURRENTLY active tenant context.
     * Called from services (InventoryNotificationService, StockSummaryService) that
     * already have the correct tenant set in ThreadLocal.
     */
    public Double getEffectiveReorderLevel(Product product) {
        return configRepo.findByProductId(product.getProductId())
                .map(ProductTenantConfig::getReorderLevel)
                .filter(val -> val != null)
                .orElse(product.getReorderQuantity());
    }

    /**
     * Batch override map — single query returning only entries that have an override.
     * Keys not present in the map have no override (caller falls back to global value).
     * Used by StockService to avoid N+1 queries when building the stock list.
     */
    public Map<Long, Double> getOverrideMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();
        return configRepo.findByProductIds(productIds).stream()
                .filter(c -> c.getReorderLevel() != null)
                .collect(Collectors.toMap(ProductTenantConfig::getProductId,
                                         ProductTenantConfig::getReorderLevel));
    }

    /**
     * Batch version — single query for all overrides in the current tenant schema.
     */
    public Map<Long, Double> getEffectiveReorderLevels(List<Product> products) {
        if (products == null || products.isEmpty()) return Collections.emptyMap();

        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        Map<Long, Double> overrides = configRepo.findByProductIds(productIds).stream()
                .filter(c -> c.getReorderLevel() != null)
                .collect(Collectors.toMap(ProductTenantConfig::getProductId,
                                         ProductTenantConfig::getReorderLevel));

        return products.stream().collect(Collectors.toMap(
                Product::getProductId,
                p -> overrides.getOrDefault(p.getProductId(), p.getReorderQuantity())
        ));
    }

    /**
     * Cross-tenant bulk resolution — returns effective reorder level per tenant per product.
     * Result: tenantSchema → (productId → effectiveReorderLevel)
     *
     * Switches into each tenant schema using TenantSchemaExecutor (REQUIRES_NEW) so every
     * product_tenant_config query runs in the correct DB schema.
     * The original ThreadLocal context is always restored in the finally block.
     */
    public Map<String, Map<Long, Double>> getEffectiveReorderLevelsByTenant(List<Product> products) {
        if (products == null || products.isEmpty()) return Collections.emptyMap();

        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        // Global defaults — used when a tenant has no override
        Map<Long, Double> globalDefaults = products.stream()
                .collect(Collectors.toMap(Product::getProductId,
                                          p -> p.getReorderQuantity() != null ? p.getReorderQuantity() : 0.0,
                                          (a, b) -> a));

        String originalTenant = ThreadLocalStorage.getTenantName();
        Map<String, Map<Long, Double>> result = new LinkedHashMap<>();
        try {
            for (String tenantSchema : tenants) {
                ThreadLocalStorage.setTenantName(tenantSchema);
                // REQUIRES_NEW picks up the ThreadLocal we just set
                Map<Long, Double> overrides = tenantSchemaExecutor.findOverridesForProducts(productIds);

                Map<Long, Double> effective = new HashMap<>();
                for (Long productId : productIds) {
                    Double override = overrides.get(productId);
                    effective.put(productId, override != null ? override : globalDefaults.get(productId));
                }
                result.put(tenantSchema, effective);
            }
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }
        return result;
    }

    // ── Cross-tenant management APIs (called from ProductController) ─────────────

    /**
     * Returns the reorder-level config for ALL non-master tenants for a given product.
     * Each tenant schema is queried in isolation via TenantSchemaExecutor (REQUIRES_NEW).
     */
    public List<AllTenantReorderConfigDTO> getAllTenantConfigs(Long productId, Double globalReorderLevel) {
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        List<AllTenantReorderConfigDTO> result = new ArrayList<>();

        String originalTenant = ThreadLocalStorage.getTenantName();
        try {
            for (String tenantSchema : tenants) {
                String tenantCode = schemaConfig.getSchemaCode(tenantSchema);

                // Must set ThreadLocal BEFORE the REQUIRES_NEW call; the routing
                // DataSource reads it when it acquires the connection for the new tx.
                ThreadLocalStorage.setTenantName(tenantSchema);

                Optional<ProductTenantConfig> config =
                        tenantSchemaExecutor.findConfigInCurrentTenant(productId);

                Double override = config
                        .map(ProductTenantConfig::getReorderLevel)
                        .filter(v -> v != null)
                        .orElse(null);

                result.add(new AllTenantReorderConfigDTO(
                        tenantSchema, tenantCode, globalReorderLevel, override, override != null));
            }
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }
        return result;
    }

    /**
     * Saves the reorder-level override for one specific tenant.
     */
    public void saveOverrideForTenant(Long productId, String tenantName, Double reorderLevel) {
        validateTenant(tenantName);
        String originalTenant = ThreadLocalStorage.getTenantName();
        try {
            ThreadLocalStorage.setTenantName(tenantName);
            tenantSchemaExecutor.saveConfigInCurrentTenant(productId, reorderLevel);
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }
    }

    /**
     * Removes the override for one specific tenant (falls back to global reorder level).
     */
    public void removeOverrideForTenant(Long productId, String tenantName) {
        validateTenant(tenantName);
        String originalTenant = ThreadLocalStorage.getTenantName();
        try {
            ThreadLocalStorage.setTenantName(tenantName);
            tenantSchemaExecutor.removeConfigInCurrentTenant(productId);
        } finally {
            ThreadLocalStorage.setTenantName(originalTenant);
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────────

    private void validateTenant(String tenantName) {
        if (!schemaConfig.isValidSchema(tenantName)) {
            throw new IllegalArgumentException("Unknown tenant schema: " + tenantName);
        }
        if (schemaConfig.getMasterSchema().equalsIgnoreCase(tenantName)) {
            throw new IllegalArgumentException("Cannot set reorder override on master schema");
        }
    }
}
