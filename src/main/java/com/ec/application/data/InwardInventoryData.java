package com.ec.application.data;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.lang.NonNull;

import com.ec.application.model.BasicEntities.Product;
import com.ec.application.model.BasicEntities.UnloadingArea;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class InwardInventoryData 
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	Date Date;
	@NonNull
	Long vendorId;
	@NonNull
	Long productId;
	@NonNull
	Float quantity;
	String vehicleNo;
	String vendorSlipNo;
	String ourSlipNo;
	@NonNull
	Long unloadingAreaId;
	public Date getDate() {
		return Date;
	}
	public void setDate(Date date) {
		Date = date;
	}
	public Long getVendorId() {
		return vendorId;
	}
	public void setVendorId(Long vendorId) {
		this.vendorId = vendorId;
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
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
	public Long getUnloadingAreaId() {
		return unloadingAreaId;
	}
	public void setUnloadingAreaId(Long unloadingAreaId) {
		this.unloadingAreaId = unloadingAreaId;
	}
	
	
}
