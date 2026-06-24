package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "supplier_quote_criteria_value")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class SupplierQuoteCriteriaValue extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Set when this value belongs to a LINE-scoped criteria. Null for HEADER-scoped values.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_quote_line_id", nullable = true)
    private SupplierQuoteLine supplierQuoteLine;

    // Set when this value belongs to a HEADER-scoped criteria. Null for LINE-scoped values.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_quote_id", nullable = true)
    private SupplierQuote supplierQuote;

    @Column(name = "criteria_id", nullable = false)
    private Long criteriaId;

    @Column(name = "criteria_name")
    private String criteriaName;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
}
