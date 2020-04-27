package com.ec.application.data;

import org.springframework.lang.NonNull;

public class CurrentStockRequest 
{
	@NonNull
	Long productId;
	@NonNull
	Long warehouseId;
	
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public Long getWarehouseId() {
		return warehouseId;
	}
	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}
}
