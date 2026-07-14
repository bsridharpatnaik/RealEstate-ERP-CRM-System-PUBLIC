package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "quote_comparison_line")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class QuoteComparisonLine extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_id", nullable = false)
    private QuoteComparison quoteComparison;

    @Column(name = "indent_id")
    private String indentId;

    @Column(name = "indent_line_id")
    private String indentLineId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "required_qty")
    private Double requiredQty;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "need_by_date")
    @Temporal(TemporalType.DATE)
    private Date needByDate;

    @Column(name = "line_status", nullable = false)
    private String lineStatus = "OPEN";

    @Column(name = "finalized_supplier_quote_line_id")
    private Long finalizedSupplierQuoteLineId;

    @Column(name = "finalized_by")
    private String finalizedBy;

    @Column(name = "finalized_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finalizedAt;

    @Column(name = "finalized_remarks", columnDefinition = "TEXT")
    private String finalizedRemarks;

    @Column(name = "is_non_lowest_selection")
    private Boolean isNonLowestSelection = false;
}
