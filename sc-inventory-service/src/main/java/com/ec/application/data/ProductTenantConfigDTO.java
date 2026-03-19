package com.ec.application.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductTenantConfigDTO {
    private Long productId;
    private Double globalReorderLevel;       // from Product.reorderQuantity (read-only)
    private Double overrideReorderLevel;     // tenant-specific override (null if not set)
    private Boolean isOverridden;            // true if an override is active
}
