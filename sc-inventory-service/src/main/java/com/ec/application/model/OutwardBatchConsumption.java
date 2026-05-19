package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "outward_batch_consumption")
@Data
@NoArgsConstructor
public class OutwardBatchConsumption extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "outward_id", nullable = false)
    private Long outwardId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "qty_consumed", nullable = false)
    private Double qtyConsumed;

    @Column(name = "fifo_overridden", columnDefinition = "boolean default false")
    private Boolean fifoOverridden = false;

    @Column(name = "override_comment", length = 500)
    private String overrideComment;
}
