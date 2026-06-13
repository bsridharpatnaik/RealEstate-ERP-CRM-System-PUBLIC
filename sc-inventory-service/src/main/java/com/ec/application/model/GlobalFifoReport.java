package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Master-schema table storing FIFO-override outward consumption rows
 * synced from all tenant schemas. One row per OutwardBatchConsumption
 * where fifo_overridden = true.
 */
@Entity
@Table(
    name = "global_fifo_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_fifo_tenant_outward_batch_product",
        columnNames = {"tenantSchema", "outwardId", "batchId", "productId"}
    ),
    indexes = {
        @Index(name = "idx_gfr_tenant",       columnList = "tenantSchema"),
        @Index(name = "idx_gfr_outward_date",  columnList = "outwardDate"),
        @Index(name = "idx_gfr_synced_at",     columnList = "syncedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GlobalFifoReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenantSchema;

    @Column(nullable = false)
    private Long outwardId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date outwardDate;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 50)
    private String productCode;

    @Column(length = 20)
    private String measurementUnit;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 150)
    private String warehouseName;

    /** outward_inventory.usageLocation.locationName */
    @Column(length = 200)
    private String usageLocationName;

    /** outward_inventory.usageArea.usageAreaName */
    @Column(length = 200)
    private String usageAreaName;

    /** outward_inventory.contractor.name */
    @Column(length = 200)
    private String contractorName;

    /** outward_inventory.purpose */
    @Column(length = 500)
    private String purpose;

    @Column(nullable = false)
    private Long batchId;

    @Column(length = 200)
    private String batchLotNumber;

    @Column(length = 200)
    private String batchBrand;

    /** stored as dd-MM-yyyy */
    @Column(length = 20)
    private String batchReceivedDate;

    /** stored as dd-MM-yyyy; null for BATCH_ONLY products */
    @Column(length = 20)
    private String batchExpiryDate;

    @Column(nullable = false)
    private Double qtyConsumed;

    @Column(length = 500)
    private String overrideComment;

    /** outward_inventory.createdBy */
    @Column(length = 100)
    private String performedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMM yyyy, hh:mm a", timezone = "Asia/Kolkata")
    @Column(nullable = false)
    private Date syncedAt;
}
