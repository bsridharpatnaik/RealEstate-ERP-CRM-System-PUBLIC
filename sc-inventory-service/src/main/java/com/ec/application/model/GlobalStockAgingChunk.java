package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table: one row per FIFO "chunk" of stock still present for a
 * (tenantSchema, productId, warehouseId) — i.e. one row per inward batch that has not
 * yet been fully consumed, oldest first. Powers the age-breakdown expand in the UI,
 * mirroring the per-project Stock page's FIFO aging display.
 */
@Entity
@Table(
    name = "global_stock_aging_chunk",
    indexes = {
        @Index(name = "idx_sac_tenant_product_warehouse", columnList = "tenantSchema, productId, warehouseId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalStockAgingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenantSchema;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Double quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    private Date inwardDate;

    @Column(nullable = false)
    private Integer ageDays;

    /** Oldest chunk = 0, ascending — preserves FIFO order for display. */
    @Column(nullable = false)
    private Integer sortOrder;
}
