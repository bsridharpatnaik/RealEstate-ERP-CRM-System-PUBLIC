package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Immutable
@Subselect("Select * from MissingInventoryPricingByMonth")
@Audited
@Data
public class MissingInventoryPricing {

    @Id
    @Column(name = "id")
    String id;
    
    String categoryName;
    Long productId;
    String productName;
    String date;
}
