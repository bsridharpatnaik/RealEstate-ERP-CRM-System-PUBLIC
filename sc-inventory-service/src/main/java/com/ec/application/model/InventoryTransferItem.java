package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.datasync.MultiTableSyncListener;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "inventory_transfer_item")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransferItem extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long transferItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    @JsonBackReference
    private InventoryTransfer inventoryTransfer;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private String measurementUnit;

    @Column(nullable = false)
    private Double quantity;
}