package com.ec.application.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Getter
@Setter
@Immutable
@Subselect("SELECT * FROM masterschema.IndentsForInward")
@Synchronize({
        "masterschema.indent_inventory",
        "masterschema.indent_inventory_entries",
        "masterschema.purchase_order",
        "masterschema.contacts",
        "masterschema.product",
        "masterschema.indent_inward_mapping"
})
public class IndentsForInwardView {

    // ---------- Identity ----------
    @Id
    @Column(name = "lineItemCode")
    private String lineItemCode;

    // ---------- Indent ----------
    @Column(name = "indent_id")
    private String indentId;

    @Temporal(TemporalType.DATE)
    @Column(name = "indent_date")
    private Date indentDate;

    @Column(name = "indent_status")
    private String indentStatus;

    @Column(name = "indentCreatedBy")
    private String indentCreatedBy;

    @Column(name = "tenant")
    private String tenant;

    // ---------- Line Item ----------
    @Column(name = "line_item_status")
    private String lineItemStatus;

    @Column(name = "productId")
    private Long productId;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "specification")
    private String specification;

    // ---------- Product ----------
    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "measurementUnit")
    private String measurementUnit;

    // ---------- Purchase Order ----------
    @Column(name = "purchaseOrderId")
    private String purchaseOrderId;

    @Temporal(TemporalType.DATE)
    @Column(name = "po_date")
    private Date poDate;

    @Column(name = "purchase_order_id")
    private String purchaseOrderNumber;

    @Column(name = "grandTotal")
    private BigDecimal grandTotal;

    @Column(name = "po_status")
    private String poStatus;

    // ---------- Supplier ----------
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name")
    private String supplierName;

    // ---------- Inward Aggregation ----------
    @Column(name = "total_inward_quantity")
    private Double totalInwardQuantity;

    // ---------- PO Line Tolerance ----------
    @Column(name = "poLineQuantity")
    private Double poLineQuantity;

    @Column(name = "tolerancePercent")
    private Double tolerancePercent;
}
