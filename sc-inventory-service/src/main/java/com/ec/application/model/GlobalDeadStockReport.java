package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table. One row per (tenantSchema, productId, batchId).
 * For non-batch-tracked products, batchId is null (one row per product).
 * Synced every 30 min from all tenant schemas — dead stock warehouse rows only.
 */
@Entity
@Table(
    name = "global_dead_stock_report",
    indexes = {
        @Index(name = "idx_dsr_tenant", columnList = "tenantSchema"),
        @Index(name = "idx_dsr_product", columnList = "productId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalDeadStockReport {

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

    /** Total qty in Dead Stock Warehouse for this product */
    @Column(nullable = false)
    private Double qty;

    /** Last PO rate for this product — used to compute stock value = qty × lastPoRate */
    private Double lastPoRate;

    // --- Batch fields (null when product is not batch-tracked) ---

    private Long batchId;

    @Column(length = 100)
    private String batchLotNumber;

    @Column(length = 100)
    private String batchBrand;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date batchReceivedDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date batchExpiryDate;

    /** Remaining qty in this specific batch (= qty for non-batch rows) */
    private Double batchQtyRemaining;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date syncedAt;
}
