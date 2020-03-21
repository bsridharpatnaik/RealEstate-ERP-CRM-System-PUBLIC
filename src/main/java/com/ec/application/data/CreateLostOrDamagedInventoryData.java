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
	Float quantity;
	
	@NonNull
	String theftLocation;

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

	public String getTheftLocation() {
		return theftLocation;
	}

	public void setTheftLocation(String theftLocation) {
		this.theftLocation = theftLocation;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	
}
