package com.ec.application.data;

public class IdNameAndUnit 
{

	Long ProductId;
	String productName;
	String measurementUnit;
	
	
	public IdNameAndUnit(Long productId, String productName, String measurementUnit) {
		super();
		ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
	}
	public Long getProductId() {
		return ProductId;
	}
	public void setProductId(Long productId) {
		ProductId = productId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getMeasurementUnit() {
		return measurementUnit;
	}
	public void setMeasurementUnit(String measurementUnit) {
		this.measurementUnit = measurementUnit;
	}
}
