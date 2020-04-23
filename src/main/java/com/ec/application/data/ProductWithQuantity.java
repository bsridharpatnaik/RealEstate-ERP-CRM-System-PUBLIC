package com.ec.application.data;

import org.springframework.lang.NonNull;

public class ProductWithQuantity 
{
	@NonNull
	Long productId;
	@NonNull
	Float quantity;
	
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
}
