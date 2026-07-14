package com.ec.application.service;

import com.ec.application.model.Product;
import org.springframework.stereotype.Component;

/**
 * Resolves the effective lead time (in calendar days) for a product.
 * Resolution order: product-level → category-level → null (not configured).
 */
@Component
public class LeadTimeResolver {

    /**
     * Returns the effective lead time for the given product.
     * Returns null if neither the product nor its category has a lead time configured.
     */
    public Integer resolve(Product product) {
        if (product == null) return null;
        if (product.getLeadTimeDays() != null) return product.getLeadTimeDays();
        if (product.getCategory() != null && product.getCategory().getLeadTimeDays() != null)
            return product.getCategory().getLeadTimeDays();
        return null;
    }
}
