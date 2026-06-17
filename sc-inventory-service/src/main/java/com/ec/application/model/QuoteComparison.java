package com.ec.application.model;

import com.ec.application.IDGenerator.GlobalQuoteComparisonIdGenerator;
import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "quote_comparison")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class QuoteComparison extends ReusableFields {

    @Id
    @GeneratedValue(generator = "qc-id-gen")
    @GenericGenerator(name = "qc-id-gen", strategy = "com.ec.application.IDGenerator.GlobalQuoteComparisonIdGenerator")
    @Column(name = "qc_id", length = 20)
    private String qcId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "comparison_date")
    @Temporal(TemporalType.DATE)
    private Date comparisonDate;

    @Column(name = "project")
    private String project;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by_user")
    private String createdByUser;

    // Indent references (comma-separated indent IDs)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_comparison_indent_refs",
            joinColumns = @JoinColumn(name = "qc_id"))
    @Column(name = "indent_id")
    private List<String> indentIds = new ArrayList<>();

    @OneToMany(mappedBy = "quoteComparison", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<QuoteComparisonLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "quoteComparison", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ComparisonCriteria> criteria = new ArrayList<>();

    @OneToMany(mappedBy = "quoteComparison", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SupplierQuote> supplierQuotes = new ArrayList<>();
}
