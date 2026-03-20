package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the reorder-level configuration for ONE tenant for a given product.
 * A list of these is returned by GET /product/{id}/all-tenant-reorder-configs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllTenantReorderConfigDTO {

    /** Database schema name used as tenant identifier, e.g. "drgtrdcntr" */
    private String tenantSchema;

    /** Human-readable short code, e.g. "DTC" — shown in the UI */
    private String tenantCode;

    /** Global reorder level from Product.reorderQuantity (read-only) */
    private Double globalReorderLevel;

    /** Tenant-specific override stored in this tenant's product_tenant_config table.
     *  Null when no override has been set. */
    private Double overrideReorderLevel;

    /** True when overrideReorderLevel is non-null */
    private Boolean isOverridden;
}
