package com.ec.application.repository;

import com.ec.application.model.ProductTenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductTenantConfigRepository extends JpaRepository<ProductTenantConfig, Long> {

    Optional<ProductTenantConfig> findByProductId(Long productId);

    @Query("SELECT p FROM ProductTenantConfig p WHERE p.productId IN :productIds")
    List<ProductTenantConfig> findByProductIds(@Param("productIds") List<Long> productIds);
}
