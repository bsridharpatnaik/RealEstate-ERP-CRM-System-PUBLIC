package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table: one rolled-up row per (tenantSchema, productId).
 * Synced hourly from all tenant schemas.
 * Rows with qty = 0 are soft-deleted (isDeleted = true).
 */
@Entity
@Table(
    name = "global_stock_aging_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sar_tenant_product",
        columnNames = {"tenantSchema", "productId"}
    ),
    indexes = {
        @Index(name = "idx_sar_tenant",       columnList = "tenantSchema"),
        @Index(name = "idx_sar_bucket",        columnList = "agingBucket"),
        @Index(name = "idx_sar_synced_at",     columnList = "syncedAt"),
        @Index(name = "idx_sar_deleted",       columnList = "isDeleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalStockAgingReport {

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

    /** Sum of quantityInHand across all warehouses for this product in this tenant. */
    @Column(nullable = false)
    private Double totalQtyInHand;

    /** Days since the most recent inward date across all warehouses (newest inward = lowest age). */
    @Column(nullable = false)
    private Integer minAgingDays;

    /** Most recent inward date across all warehouses — the reference date for minAgingDays. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date lastInwardDate;

    /** 0-30 / 31-60 / 61-90 / 90+ */
    @Column(nullable = false, length = 10)
    private String agingBucket;

    /** effectiveRate × totalQtyInHand. Null if no PO found for this product. */
    private Double pog;

    /** rate - (rate * discountPercent/100) + (rate * gstPercent/100) from last PO line. */
    private Double lastPoRate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date lastPoDate;

    /** Soft-delete flag: true when totalQtyInHand drops to 0. */
    @Column(nullable = false)
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date syncedAt;
}
