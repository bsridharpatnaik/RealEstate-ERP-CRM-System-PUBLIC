package com.ec.application.data;

import org.springframework.lang.NonNull;

public class ProductCreateData 
{
	Long categoryId;
	@NonNull
	String productName;
	String productDescription;
	@NonNull
	Double reorderQuantity;
	String measurementUnit;
	
	public Double getReorderQuantity() {
		return reorderQuantity;
	}
	public void setReorderQuantity(Double reorderQuantity) {
		this.reorderQuantity = reorderQuantity;
	}
	public Long getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}
	
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getProductDescription() {
		return productDescription;
	}
	public void setProductDescription(String productDescription) {
		this.productDescription = productDescription;
	}
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
	}
	
}
