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
     * How many base units equal 1 billing unit.
     * E.g. if base unit is Sq Ft and billing unit is Box: conversionFactor = 12
     * means 1 Box = 12 Sq Ft.
     */
    @Column(name = "conversion_factor", nullable = false)
    private Double conversionFactor;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
