package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from stockInformation")
@Immutable
@Audited
@Data
public class StockInformationFromView {
    @Id
    Long productId;

    @Column(name="product_name")
    String productName;

    @Column(name="reorderQuantity")
    Double reorderQuantity;
    @Column(name="measurementUnit")
    String measurementUnit;
    @Column(name="category_name")
    String categoryName;
    @Column(name="totalQuantityInHand")
    Double totalQuantityInHand;
    @Column(name="stockStatus")
    String stockStatus;
    @Column(name="detailedStock")
    String detailedStock;
}
