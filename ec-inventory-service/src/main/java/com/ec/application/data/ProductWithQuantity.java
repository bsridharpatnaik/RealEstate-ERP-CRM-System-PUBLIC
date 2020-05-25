package com.ec.application.data;

import org.springframework.lang.NonNull;

public class ProductWithQuantity 
{
	@NonNull
	Long productId;
	@NonNull
	Double quantity;
	
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
}
