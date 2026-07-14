package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "quote_to_po_ref")
@Data
public class QuoteToPoRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_quote_line_id", nullable = false)
    private Long supplierQuoteLineId;

    @Column(name = "qc_line_id", nullable = false)
    private Long qcLineId;

    @Column(name = "qc_id")
    private String qcId;

    @Column(name = "purchase_order_id")
    private String purchaseOrderId;

    @Column(name = "po_line_id")
    private Long poLineId;

    @Column(name = "linked_by")
    private String linkedBy;

    @Column(name = "linked_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date linkedAt;
}
