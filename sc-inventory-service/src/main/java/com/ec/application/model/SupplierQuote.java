package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "supplier_quote")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class SupplierQuote extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_id", nullable = false)
    private QuoteComparison quoteComparison;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "quotation_ref_no")
    private String quotationRefNo;

    @Column(name = "quotation_date")
    @Temporal(TemporalType.DATE)
    private Date quotationDate;

    @Column(name = "validity_date")
    @Temporal(TemporalType.DATE)
    private Date validityDate;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "freight_terms")
    private String freightTerms;

    @Column(name = "delivery_lead_days")
    private Integer deliveryLeadDays;

    @Column(name = "header_notes", columnDefinition = "TEXT")
    private String headerNotes;

    @Column(name = "created_by_user")
    private String createdByUser;

    @OneToMany(mappedBy = "supplierQuote", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<SupplierQuoteLine> lines = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "supplier_quote_fileinformation",
            joinColumns = {@JoinColumn(name = "supplier_quote_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")}
    )
    private Set<FileInformation> fileInformations = new HashSet<>();
}
