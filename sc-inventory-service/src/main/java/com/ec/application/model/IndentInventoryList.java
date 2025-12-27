package com.ec.application.model;

import javax.persistence.*;

import lombok.*;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

@Entity
@Table(name = "indent_inventory_entries")
@Audited
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class IndentInventoryList extends ReusableFields {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long entryid;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "productId", nullable = false)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    Product product;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "quantity")
    Double quantity;

    @Column(name="specification")
    String specification;

    @Column(name="remarks")
    String remarks;

    @Column(name="measurement_unit")
    String measurementUnit;

    @Column(name="line_item_status")
    String lineItemStatus;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndentInventoryList)) return false;

        IndentInventoryList that = (IndentInventoryList) o;

        return Objects.equals(product.getProductId(), that.product.getProductId()) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(specification, that.specification) &&
                Objects.equals(remarks, that.remarks) &&
                Objects.equals(measurementUnit, that.measurementUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                product.getProductId(),
                quantity,
                specification,
                remarks,
                measurementUnit
        );
    }
}
