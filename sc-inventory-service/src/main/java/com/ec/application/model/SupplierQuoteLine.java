package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "supplier_quote_line")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class SupplierQuoteLine extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_quote_id", nullable = false)
    private SupplierQuote supplierQuote;

    @Column(name = "qc_line_id", nullable = false)
    private Long qcLineId;

    @Column(name = "quoted_qty")
    private Double quotedQty;

    @Column(name = "quoted_rate")
    private Double quotedRate;

    @Column(name = "discount_percent")
    private Double discountPercent = 0.0;

    @Column(name = "gst_percent")
    private Double gstPercent = 0.0;

    @Column(name = "freight_amount")
    private Double freightAmount = 0.0;

    @Column(name = "landed_cost")
    private Double landedCost;

    @Column(name = "expected_delivery_date")
    @Temporal(TemporalType.DATE)
    private Date expectedDeliveryDate;

    @Column(name = "line_remarks", columnDefinition = "TEXT")
    private String lineRemarks;

    @OneToMany(mappedBy = "supplierQuoteLine", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<SupplierQuoteCriteriaValue> criteriaValues = new ArrayList<>();
}
