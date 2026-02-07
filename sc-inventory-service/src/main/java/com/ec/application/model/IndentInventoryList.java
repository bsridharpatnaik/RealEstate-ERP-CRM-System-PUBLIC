package com.ec.application.model;

import javax.persistence.*;

import lombok.*;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnore;  // ADD THIS IMPORT
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity(name = "IndentInventoryList")
@Table(name = "indent_inventory_entries")
@Audited
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class IndentInventoryList extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long entryid;

    // Unique identifier for the line item
    @Column(name = "line_item_code", unique = true, nullable = false, length = 50)
    private String lineItemCode;

    // To track split lineage
    @Column(name = "parent_line_item_code", length = 50)
    private String parentLineItemCode;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "productId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    Product product;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "quantity")
    Double quantity;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "quantity_received")
    Double quantityReceived;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "quantity_pending")
    Double quantityPending;

    @Column(name = "specification")
    String specification;

    @Column(name = "remarks")
    String remarks;

    @Column(name = "measurement_unit")
    String measurementUnit;

    @Column(name = "line_item_status")
    String lineItemStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indent_id", nullable = false)
    @JsonIgnore
    private IndentInventory indentInventory;

    private String purchaseOrderId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "indent_inward_mapping",
            joinColumns = @JoinColumn(
                    name = "indent_entry_id",
                    referencedColumnName = "entryid"
            )
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<IndentInwardEntry> inwardEntries = new HashSet<>();

    // ----------------- equals & hashCode -----------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndentInventoryList)) return false;
        IndentInventoryList that = (IndentInventoryList) o;
        return entryid != null && entryid.equals(that.entryid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}