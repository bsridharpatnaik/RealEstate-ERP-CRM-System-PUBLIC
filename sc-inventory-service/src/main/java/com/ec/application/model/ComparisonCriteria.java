package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = "comparison_criteria")
@Data
@EqualsAndHashCode(callSuper = false)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class ComparisonCriteria extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_id", nullable = false)
    private QuoteComparison quoteComparison;

    @Column(name = "criteria_name", nullable = false)
    private String criteriaName;

    // NUMBER, TEXT, DATE, BOOLEAN
    @Column(name = "criteria_type", nullable = false)
    private String criteriaType = "TEXT";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = false;
}
