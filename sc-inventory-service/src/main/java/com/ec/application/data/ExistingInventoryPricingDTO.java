package com.ec.application.data;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
public class ExistingInventoryPricingDTO {
    String id;
    String categoryName;
    Long productId;
    String productName;
    String date;
    Double price;
}
