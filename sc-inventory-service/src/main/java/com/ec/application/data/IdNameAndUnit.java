package com.ec.application.data;

import lombok.Data;

@Data
public class IdNameAndUnit 
{

	Long ProductId;
	String productName;
	String measurementUnit;
	String productCode;
	
	
	public IdNameAndUnit(Long productId, String productName, String measurementUnit, String productCode) {
		super();
		ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
		this.productCode = productCode;
	}
}
