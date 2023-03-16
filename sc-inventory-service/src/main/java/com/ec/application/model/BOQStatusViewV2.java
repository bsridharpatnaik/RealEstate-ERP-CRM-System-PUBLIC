package com.ec.application.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from boq_status_view2")
@Immutable
//@Audited
@Data
public class BOQStatusViewV2 {

    @Id
    Long id;

    @Column(name="buildingTypeId")
    Long buildingTypeId;

    @Column(name="building_type")
    String buildingType;

    @Column(name="location_id")
    Long locationId;

    @Column(name="location_name")
    String locationName;

    @Column(name="productId")
    Long productId;

    @Column(name="product_name")
    String productName;

    @Column(name="category_name")
    String categoryName;

    @Column(name="total_boq_quantity")
    Double totalBoqQuantity;

    @Column(name="total_outward_quantity")
    Double totalOutwardQuantity;

    @Column(name="detailed")
    String detailed;

    @Column(name="status")
    Double status;

    @Column(name="statusBucket")
    String statusBucket;
}
