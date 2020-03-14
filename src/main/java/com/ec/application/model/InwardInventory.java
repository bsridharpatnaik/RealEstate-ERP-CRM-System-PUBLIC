package com.ec.application.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import org.springframework.lang.NonNull;

import com.ec.application.SoftDelete.SoftDeletableEntity;
import com.ec.application.model.BasicEntities.Product;
import com.ec.application.model.BasicEntities.UnloadingArea;
import com.ec.application.model.BasicEntities.Vendor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "inward_inventory")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class InwardInventory extends SoftDeletableEntity
{

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long Id;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date Date;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="vendorId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Vendor vendor;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="productId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Product product;
	
	@NonNull
	Float quantity;
	
	@NonNull
	String vehicleNo;
	
	String vendorSlipNo;
	
	String ourSlipNo;
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="unloadingAreaId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	UnloadingArea unloadingArea;
	
	
	public Long getId() {
		return Id;
	}
	public void setId(Long id) {
		Id = id;
	}
	public Date getDate() {
		return Date;
	}
	public void setDate(Date date) {
		Date = date;
	}
	public Vendor getVendor() {
		return vendor;
	}
	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public Float getQuantity() {
		return quantity;
	}
	public void setQuantity(Float quantity) {
		this.quantity = quantity;
	}
	public String getVehicleNo() {
		return vehicleNo;
	}
	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}
	public String getVendorSlipNo() {
		return vendorSlipNo;
	}
	public void setVendorSlipNo(String vendorSlipNo) {
		this.vendorSlipNo = vendorSlipNo;
	}
	public String getOurSlipNo() {
		return ourSlipNo;
	}
	public void setOurSlipNo(String ourSlipNo) {
		this.ourSlipNo = ourSlipNo;
	}
	public UnloadingArea getUnloadingArea() {
		return unloadingArea;
	}
	public void setUnloadingArea(UnloadingArea unloadingArea) {
		this.unloadingArea = unloadingArea;
	}
}
