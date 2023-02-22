package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Immutable
@Subselect("Select * from InventoryMonthUsageInformation")
@Data
public class InventoryMonthUsageInformation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    String id;

    @Column(name = "locationId")
    Long locationId;

    @Column(name = "locationName")
    String locationName;

    @Column(name = "categoryId")
    Long categoryId;

    @Column(name = "categoryName")
    String categoryName;

    @Column(name = "productId")
    Long productId;

    @Column(name = "productName")
    String productName;

    @Column(name = "ym")
    String ym;

    @Column(name = "totalQuantity")
    Double totalQuantity;

    @Column(name = "price")
    Double price;

    @Column(name = "totalPrice")
    Double totalPrice;
}
