package com.ec.application.model;


import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ec.application.ReusableClasses.ReusableFields;
@Entity
@Table(name = "BOQUpload")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class BOQUpload extends ReusableFields {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	private int sno;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinColumn(name = "buildingTypeId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	private BuildingType buildingType;
 
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinColumn(name = "productId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Product product;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinColumn(name = "locationId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	UsageArea location;
	
	
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
	@JoinColumn(name = "usageLocationId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	UsageLocation usageLocation;
	
    private double quantity; 
	
	private String changes;
	
	
	public UsageLocation getUsageLocation() {
		return usageLocation;
	}
	public void setUsageLocation(UsageLocation usageLocation) {
		this.usageLocation = usageLocation;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getSno() {
		return sno;
	}
	public void setSno(int sno) {
		this.sno = sno;
	}
	
	public BuildingType getBuildingType() {
		return buildingType;
	}
	public void setBuildingType(BuildingType buildingType) {
		this.buildingType = buildingType;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	
	public double getQuantity() {
		return quantity;
	}
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}
	public String getChanges() {
		return changes;
	}
	public void setChanges(String changes) {
		this.changes = changes;
	}
	public UsageArea getLocation() {
		return location;
	}
	public void setLocation(UsageArea location) {
		this.location = location;
	}
	
	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}
	
	
}
