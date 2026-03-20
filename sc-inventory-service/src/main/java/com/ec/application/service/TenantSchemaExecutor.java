package com.ec.application.service;

import com.ec.application.model.ProductTenantConfig;
import com.ec.application.repository.ProductTenantConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Executes ProductTenantConfig DB operations inside a fresh REQUIRES_NEW transaction.
 *
 * Why a separate bean?  Spring AOP proxies only intercept calls made from *other* beans.
 * If ProductTenantConfigService called its own @Transactional(REQUIRES_NEW) methods
 * via `this.method()`, the proxy would be bypassed and no schema switch would happen.
 *
 * Why set ThreadLocal BEFORE calling these methods (in the caller)?
 * AbstractRoutingDataSource.determineCurrentLookupKey() is called when Spring's
 * transaction proxy acquires the DB connection — i.e., *before* the method body runs.
 * So the ThreadLocal must be set by the caller prior to the method call.
 */
@Component
@RequiredArgsConstructor
public class TenantSchemaExecutor {

    private final ProductTenantConfigRepository configRepo;

    /**
     * Finds the tenant config for a product in whichever schema is currently
     * set in ThreadLocal by the caller.  Runs in its own transaction so that
     * AbstractRoutingDataSource picks up the already-switched ThreadLocal tenant.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ProductTenantConfig> findConfigInCurrentTenant(Long productId) {
        return configRepo.findByProductId(productId);
    }

    /**
     * Saves (upsert) the reorder-level override for a product in whichever schema
     * is currently set in ThreadLocal by the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveConfigInCurrentTenant(Long productId, Double reorderLevel) {
        ProductTenantConfig config = configRepo.findByProductId(productId)
                .orElse(new ProductTenantConfig(productId, null));
        config.setReorderLevel(reorderLevel);
        configRepo.save(config);
    }

    /**
     * Clears the reorder-level override (sets it to null) in whichever schema
     * is currently set in ThreadLocal by the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeConfigInCurrentTenant(Long productId) {
        configRepo.findByProductId(productId).ifPresent(config -> {
            config.setReorderLevel(null);
            configRepo.save(config);
        });
    }
}
