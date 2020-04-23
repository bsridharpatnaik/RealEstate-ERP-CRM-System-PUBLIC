package com.ec.application.data;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;

public class InwardInventoryData 
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@NonNull
	Date Date;
	@NonNull
	Long supplierId;
	@NonNull
	Long warehouseId;
	@NonNull
	List<ProductWithQuantity> productWithQuantities;
	String vehicleNo;
	String vendorSlipNo;
	String ourSlipNo;
	String additionalComments;
	public Date getDate() {
		return Date;
	}
	public void setDate(Date date) {
		Date = date;
	}
	public Long getSupplierId() {
		return supplierId;
	}
	public void setSupplierId(Long supplierId) {
		this.supplierId = supplierId;
	}
	public Long getWarehouseId() {
		return warehouseId;
	}
	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}
	public List<ProductWithQuantity> getProductWithQuantities() {
		return productWithQuantities;
	}
	public void setProductWithQuantities(List<ProductWithQuantity> productWithQuantities) {
		this.productWithQuantities = productWithQuantities;
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
	public String getAdditionalComments() {
		return additionalComments;
	}
	public void setAdditionalComments(String additionalComments) {
		this.additionalComments = additionalComments;
	}
}
