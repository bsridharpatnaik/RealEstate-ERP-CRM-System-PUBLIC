package com.ec.application.data;

import java.util.List;

import org.springframework.lang.NonNull;

public class CurrentStockRequest 
{
	@NonNull
	List<Long> productIds;
	@NonNull
	Long warehouseId;
	public List<Long> getProductIds() {
		return productIds;
	}
	public void setProductIds(List<Long> productIds) {
		this.productIds = productIds;
	}
	public Long getWarehouseId() {
		return warehouseId;
	}
	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}
}
