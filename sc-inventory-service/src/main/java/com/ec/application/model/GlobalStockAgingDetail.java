package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Master-schema table: one row per (tenantSchema, productId, warehouseId).
 * Powers the per-warehouse breakdown popup in the UI.
 */
@Entity
@Table(
    name = "global_stock_aging_detail",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sad_tenant_product_warehouse",
        columnNames = {"tenantSchema", "productId", "warehouseId"}
    ),
    indexes = {
        @Index(name = "idx_sad_tenant_product", columnList = "tenantSchema, productId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalStockAgingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenantSchema;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 150)
    private String warehouseName;

    @Column(nullable = false)
    private Double qtyInHand;

    /**
     * Age, in days, of the OLDEST stock chunk still present in this warehouse — computed via a
     * FIFO walk over inward history, not simply "days since last inward." 9999 if no inward
     * history is traceable for the qty currently in hand.
     */
    @Column(nullable = false)
    private Integer agingDays;

    /** Inward date of the oldest stock chunk still present (FIFO), not the most recent inward. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date lastInwardDate;

    /** 0-30 / 31-60 / 61-90 / 90+ */
    @Column(nullable = false, length = 10)
    private String agingBucket;

    /** Not persisted — populated at read time by the controller for the expand/drill-down UI. */
    @Transient
    private List<GlobalStockAgingChunk> ageBreakdown;
}
