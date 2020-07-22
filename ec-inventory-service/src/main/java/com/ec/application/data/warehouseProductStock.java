package com.ec.application.data;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class warehouseProductStock 
{
	String warehouseName;
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Float quantityInHand;
	public String getWarehouseName() {
		return warehouseName;
	}
	public void setWarehouseName(String warehouseName) {
		this.warehouseName = warehouseName;
	}
	public Float getQuantityInHand() {
		return quantityInHand;
	}
	public void setQuantityInHand(Float quantityInHand) {
		this.quantityInHand = quantityInHand;
	}
}
