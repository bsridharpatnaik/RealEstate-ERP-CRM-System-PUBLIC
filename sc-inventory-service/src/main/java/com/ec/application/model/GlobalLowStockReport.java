package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table: one row per (tenantSchema, productId) where
 * quantityInHand < reorderLevel. Rows are removed when stock recovers.
 * Synced every 30 minutes from all tenant schemas.
 */
@Entity
@Table(
    name = "global_low_stock_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_lsr_tenant_product",
        columnNames = {"tenantSchema", "productId"}
    ),
    indexes = {
        @Index(name = "idx_lsr_tenant",          columnList = "tenantSchema"),
        @Index(name = "idx_lsr_low_stock_since",  columnList = "lowStockSince"),
        @Index(name = "idx_lsr_category",         columnList = "category")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalLowStockReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenantSchema;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 50)
    private String productCode;

    @Column(length = 20)
    private String unit;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Double qtyInHand;

    @Column(nullable = false)
    private Double reorderLevel;

    /** reorderLevel - qtyInHand — how much needs to be procured. */
    @Column(nullable = false)
    private Double deficit;

    /**
     * When this product was first detected below reorder level.
     * Preserved across syncs — only reset if product recovers and drops again.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date lowStockSince;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date syncedAt;
}
