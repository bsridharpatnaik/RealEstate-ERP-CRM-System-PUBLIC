package com.ec.application.data;

import java.util.List;

import com.ec.application.model.Stock;

public class SingleStockInfo 
{

	Long productId;
	String productName;
	String categoryName;
	Double totalQuantityInHand;
	List<Stock> detailedStock;
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}
	public Double getTotalQuantityInHand() {
		return totalQuantityInHand;
	}
	public void setTotalQuantityInHand(Double totalQuantityInHand) {
		this.totalQuantityInHand = totalQuantityInHand;
	}
	public List<Stock> getDetailedStock() {
		return detailedStock;
	}
	public void setDetailedStock(List<Stock> detailedStock) {
		this.detailedStock = detailedStock;
	}
}
