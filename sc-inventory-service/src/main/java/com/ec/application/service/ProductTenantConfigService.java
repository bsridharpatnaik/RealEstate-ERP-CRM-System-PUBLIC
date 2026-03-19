package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.data.ProductTenantConfigDTO;
import com.ec.application.model.Product;
import com.ec.application.model.ProductTenantConfig;
import com.ec.application.repository.ProductTenantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class ProductTenantConfigService {

    private final ProductTenantConfigRepository configRepo;

    /**
     * Returns the effective reorder level for a product:
     * tenant override if set, otherwise the global product value.
     */
    public Double getEffectiveReorderLevel(Product product) {
        return configRepo.findByProductId(product.getProductId())
                .map(ProductTenantConfig::getReorderLevel)
                .filter(val -> val != null)
                .orElse(product.getReorderQuantity());
    }

    /**
     * Returns a map of productId → effectiveReorderLevel for a batch of products.
     * Efficient: single query for all overrides, then falls back to global values.
     */
    public Map<Long, Double> getEffectiveReorderLevels(List<Product> products) {
        if (products == null || products.isEmpty()) return Collections.emptyMap();

        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        // Fetch all overrides in one query
        Map<Long, Double> overrides = configRepo.findByProductIds(productIds).stream()
                .filter(c -> c.getReorderLevel() != null)
                .collect(Collectors.toMap(ProductTenantConfig::getProductId,
                                         ProductTenantConfig::getReorderLevel));

        // Resolve: override first, fall back to global
        return products.stream().collect(Collectors.toMap(
                Product::getProductId,
                p -> overrides.getOrDefault(p.getProductId(), p.getReorderQuantity())
        ));
    }

    /**
     * Get current tenant config for a product (for display in UI).
     */
    public ProductTenantConfigDTO getConfig(Long productId, Double globalReorderQuantity) {
        Optional<ProductTenantConfig> config = configRepo.findByProductId(productId);
        ProductTenantConfigDTO dto = new ProductTenantConfigDTO();
        dto.setProductId(productId);
        dto.setGlobalReorderLevel(globalReorderQuantity);
        if (config.isPresent() && config.get().getReorderLevel() != null) {
            dto.setOverrideReorderLevel(config.get().getReorderLevel());
            dto.setIsOverridden(true);
        } else {
            dto.setOverrideReorderLevel(null);
            dto.setIsOverridden(false);
        }
        return dto;
    }

    /**
     * Save or update the tenant-specific reorder level override.
     */
    @Transactional
    public ProductTenantConfigDTO saveOverride(Long productId, Double reorderLevel, Double globalReorderQuantity) {
        ProductTenantConfig config = configRepo.findByProductId(productId)
                .orElse(new ProductTenantConfig(productId, null));
        config.setReorderLevel(reorderLevel);
        configRepo.save(config);
        return getConfig(productId, globalReorderQuantity);
    }

    /**
     * Remove the tenant-specific override — product falls back to global value.
     */
    @Transactional
    public void removeOverride(Long productId) {
        configRepo.findByProductId(productId).ifPresent(config -> {
            config.setReorderLevel(null);
            configRepo.save(config);
        });
    }
}
