package com.ec.application.model;

import javax.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(name = "indent_inventory_entries")
@Audited
@Data
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
}
