package com.ec.application.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import lombok.Data;

@Entity
@Subselect("select * from boq_status")
@Immutable
@Audited
@Data
public class BOQStatus implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	String id;

	@Column(name = "typeId")
	Long typeId;

	@Column(name = "building_type")
	String buildingType;

	@Column(name = "locationId")
	Long locationId;

	@Column(name = "location_name")
	String locationName;

	@Column(name = "productId")
	Long productId;

	@Column(name = "product_name")
	String productName;

	@Column(name = "totalExpectedQuantity")
	Double totalExpectedQuantity;

	@Column(name = "totalConsumedQuantity")
	Double totalConsumedQuantity;

	@Column(name = "consumedPercent")
	Double consumedPercent;
}
