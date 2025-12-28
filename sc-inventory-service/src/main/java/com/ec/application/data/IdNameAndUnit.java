package com.ec.application.data;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

@Data
public class IdNameAndUnit 
{

	Long ProductId;
	String productName;
	String measurementUnit;
	String productCode;
	Boolean isManagedInventory;
	
	
	public IdNameAndUnit(Long productId, String productName, String measurementUnit, String productCode, Boolean isManagedInventory) {
		super();
		this.ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
		this.productCode = productCode;
		this.isManagedInventory = isManagedInventory;
	}
}
