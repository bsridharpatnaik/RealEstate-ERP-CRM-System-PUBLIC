package com.ec.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "product_unit_conversions")
@Getter
@Setter
@NoArgsConstructor
public class ProductUnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "unit_name", nullable = false, length = 100)
    private String unitName;

    /**
     * ALWAYS stored canonically as: how many base units equal 1 billing unit.
     * E.g. base unit Sq Ft, billing unit Box: conversionFactor = 12 means 1 Box = 12 Sq Ft.
     * PO billing qty is computed as baseQty / conversionFactor everywhere.
     */
    @Column(name = "conversion_factor", nullable = false)
    private Double conversionFactor;

    /**
     * How the user entered/views the factor — display + edit only, never used in the PO calc.
     * BILLING_PER_BASE (default, legacy): shown as "1 {billingUnit} = {conversionFactor} {baseUnit}".
     * BASE_PER_BILLING: shown as "1 {baseUnit} = {1/conversionFactor} {billingUnit}" (e.g. "1 pc = 85 kg").
     * Null is treated as BILLING_PER_BASE for pre-existing rows.
     */
    @Column(name = "display_direction", length = 20)
    private String displayDirection = "BILLING_PER_BASE";

    /**
     * Display/edit only — the unit this conversion was defined against (null = the base unit).
     * conversionFactor is ALWAYS stored resolved-to-base, so the PO calc never walks this chain.
     * referenceValue is the number the user typed relative to referenceUnit (e.g. "10" for
     * "1 carton = 10 box"). Null referenceValue = legacy row defined directly against base.
     * Snapshot: if referenceUnit later changes, this row is NOT recomputed — re-edit it.
     */
    @Column(name = "reference_unit", length = 100)
    private String referenceUnit;

    @Column(name = "reference_value")
    private Double referenceValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
