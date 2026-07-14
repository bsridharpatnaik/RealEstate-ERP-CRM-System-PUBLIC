package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table. One row per (tenantSchema, productId, warehouseId, batchId).
 * Only covers BATCH_WITH_EXPIRY products — batches where expiryDate IS NOT NULL
 * and qtyRemaining > 0. Synced once daily.
 */
@Entity
@Table(
    name = "global_expired_stock_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_esr_tenant_product_warehouse_batch",
        columnNames = {"tenantSchema", "productId", "warehouseId", "batchId"}
    ),
    indexes = {
        @Index(name = "idx_esr_tenant",      columnList = "tenantSchema"),
        @Index(name = "idx_esr_product",     columnList = "productId"),
        @Index(name = "idx_esr_days",        columnList = "daysUntilExpiry"),
        @Index(name = "idx_esr_synced_at",   columnList = "syncedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalExpiredStockReport {

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
    private Long warehouseId;

    @Column(nullable = false, length = 150)
    private String warehouseName;

    @Column(nullable = false)
    private Long batchId;

    @Column(length = 200)
    private String lotNumber;

    @Column(length = 200)
    private String brand;

    /** Stored as dd-MM-yyyy string for easy display */
    @Column(nullable = false, length = 20)
    private String expiryDate;

    /** Stored as dd-MM-yyyy string */
    @Column(length = 20)
    private String receivedDate;

    @Column(nullable = false)
    private Double qtyRemaining;

    /**
     * Days until expiry computed at sync time.
     * Negative = already expired, 0 = expires today, positive = expires in N days.
     */
    @Column(nullable = false)
    private Integer daysUntilExpiry;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date syncedAt;
}
