package com.ec.application.data;

import java.util.List;

import com.ec.application.model.Stock;

public class SingleStockInfo 
{

	Long productId;
	Float totalQuantityInHand;
	List<Stock> detailedStock;
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public Float getTotalQuantityInHand() {
		return totalQuantityInHand;
	}
	public void setTotalQuantityInHand(Float totalQuantityInHand) {
		this.totalQuantityInHand = totalQuantityInHand;
	}
	public List<Stock> getDetailedStock() {
		return detailedStock;
	}
	public void setDetailedStock(List<Stock> detailedStock) {
		this.detailedStock = detailedStock;
	}
}
