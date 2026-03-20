package com.ec.application.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PUT /product/{id}/tenant-reorder-config.
 * Identifies which tenant's override to save, and the new value.
 */
@Data
@NoArgsConstructor
public class TenantReorderSaveRequest {

    /** Tenant schema name (must match a valid entry in SchemaConfig), e.g. "drgtrdcntr" */
    private String tenantName;

    /** The override reorder level to persist in that tenant's schema. */
    private Double reorderLevel;
}
