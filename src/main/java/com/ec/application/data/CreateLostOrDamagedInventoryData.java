package com.ec.application.data;

import java.util.Date;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;

public class CreateLostOrDamagedInventoryData 
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	private Date date;
	
	@NonNull
	Long productId;
	
	@NonNull
	Double quantity;
	
	@NonNull
	String theftLocation;
	
	@NonNull
	Long warehouseId;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Double getQuantity() {
		return quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public String getTheftLocation() {
		return theftLocation;
	}

	public void setTheftLocation(String theftLocation) {
		this.theftLocation = theftLocation;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}
}
