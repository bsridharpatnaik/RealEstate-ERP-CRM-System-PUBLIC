package com.ec.application.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Immutable
@Subselect("Select * from InventoryReport")
@Data
public class InventoryReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    String id;

    @Column(name="month")
    String month;

    @Column(name="product_name")
    String productName;

    @Column(name = "measurementunit")
    @JsonDeserialize(using = com.ec.application.Deserializers.ToUpperCaseDeserializer.class)
    String measurementunit;

    @Column(name="category_name")
    String categoryName;

    @Column(name = "warehousename")
    String warehouseName;

    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "opening_stock")
    Double openingStock;

    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "total_inward")
    Double totalInward;

    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "total_outward")
    Double totalOutward;

    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "total_lost_damaged")
    Double totalLostDamaged;

    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "closing_stock")
    Double closingStock;
}
