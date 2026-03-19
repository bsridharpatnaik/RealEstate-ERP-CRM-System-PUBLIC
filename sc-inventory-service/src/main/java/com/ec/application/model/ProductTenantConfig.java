package com.ec.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * Tenant-specific overrides for Product configuration.
 * Lives in each tenant's schema. One row per product.
 * If a row exists and reorderLevel is non-null, it takes precedence
 * over Product.reorderQuantity for this tenant.
 */
@Entity
@Table(name = "product_tenant_config")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductTenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "reorder_level")
    private Double reorderLevel;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    public ProductTenantConfig(Long productId, Double reorderLevel) {
        this.productId = productId;
        this.reorderLevel = reorderLevel;
    }
}
