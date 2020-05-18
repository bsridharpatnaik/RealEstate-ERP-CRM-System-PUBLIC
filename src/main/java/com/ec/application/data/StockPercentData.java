package com.ec.application.data;

public class StockPercentData 
{
	Long productId;
	String productName;
	Double stockPercent;
	
	
	public StockPercentData(Long productId, String productName, Double stockPercent) {
		super();
		this.productId = productId;
		this.productName = productName;
		this.stockPercent = stockPercent;
	}
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
	public Double getStockPercent() {
		return stockPercent;
	}
	public void setStockPercent(Double stockPercent) {
		this.stockPercent = stockPercent;
	}
	
	

}
