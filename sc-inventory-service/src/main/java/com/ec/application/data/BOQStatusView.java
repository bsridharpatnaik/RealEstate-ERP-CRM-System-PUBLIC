package com.ec.application.data;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import lombok.Data;

@Entity
@Subselect("select * from boq_status_view")
@Immutable
@Audited
@Data
public class BOQStatusView {

	@Id
	Long id;
	
	@Column(name="productId")
    Long productId;

    @Column(name="category")
    String category;

    @Column(name="product")
    String product;
    
    @Column(name="boqQuantity")
	Double boqQuantity;
    
    @Column(name="usageLocationId")
    Long usageLocationId;
    
    @Column(name="buildingTypeId")
	Long buildingTypeId;
     
    @Column(name="buildingType")
	String buildingType;
    
    @Column(name="buildingUnit")
	String buildingUnit;
    
    @Column(name="outwardQuantity")
	Double outwardQuantity;
    
    @Column(name="status")
	int status;
    
}
