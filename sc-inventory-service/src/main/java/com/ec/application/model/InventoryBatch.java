package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "inventory_batch")
@Data
@NoArgsConstructor
public class InventoryBatch extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "batch_id")
    private Long batchId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse warehouse;

    @Column(name = "inward_id", nullable = false)
    private Long inwardId;

    @Column(name = "brand")
    private String brand;

    /** Supplier lot / batch reference — useful for traceability in construction projects. */
    @Column(name = "lot_number")
    private String lotNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "expiry_date")
    private Date expiryDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "received_date", nullable = false)
    private Date receivedDate;

    @Column(name = "qty_received", nullable = false)
    private Double qtyReceived;

    @Column(name = "qty_remaining", nullable = false)
    private Double qtyRemaining;

    @Column(name = "alert_sent_60", columnDefinition = "boolean default false")
    private Boolean alertSent60 = false;

    @Column(name = "alert_sent_30", columnDefinition = "boolean default false")
    private Boolean alertSent30 = false;

    @Column(name = "alert_sent_expired", columnDefinition = "boolean default false")
    private Boolean alertSentExpired = false;

    @Transient
    private Long daysUntilExpiry;

    @Transient
    private Boolean isExpired;
}
