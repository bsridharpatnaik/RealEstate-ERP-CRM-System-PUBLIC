package com.ec.application.ReusableClasses;

import java.io.Serializable;

public class ProductIdAndStockProjection implements Serializable
{
	Long productId;
	Double stock;
	
	public ProductIdAndStockProjection(Long productId, Double stock) {
		super();
		this.productId = productId;
		this.stock = stock;
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public Double getStock() {
		return stock;
	}
	public void setStock(Double stock) {
		this.stock = stock;
	}
	
	
}
