package com.ec.application.data;

import java.util.List;

import com.ec.application.model.Stock;

public class SingleStockInfo
{

	Long productId;
	String productName;
	String categoryName;
	Double reorderQuantity;
	String totalQuantityInHand;
	String stockStatus;
	List<Stock> detailedStock;

	public Double getReorderQuantity()
	{
		return reorderQuantity;
	}

	public void setReorderQuantity(Double reorderQuantity)
	{
		this.reorderQuantity = reorderQuantity;
	}

	public String getStockStatus()
	{
		return stockStatus;
	}

	public void setStockStatus(String stockStatus)
	{
		this.stockStatus = stockStatus;
	}

	public Long getProductId()
	{
		return productId;
	}

	public void setProductId(Long productId)
	{
		this.productId = productId;
	}

	public String getProductName()
	{
		return productName;
	}

	public void setProductName(String productName)
	{
		this.productName = productName;
	}

	public String getCategoryName()
	{
		return categoryName;
	}

	public void setCategoryName(String categoryName)
	{
		this.categoryName = categoryName;
	}

	public String getTotalQuantityInHand()
	{
		return totalQuantityInHand;
	}

	public void setTotalQuantityInHand(String totalQuantityInHand)
	{
		this.totalQuantityInHand = totalQuantityInHand;
	}

	public List<Stock> getDetailedStock()
	{
		return detailedStock;
	}

	public void setDetailedStock(List<Stock> detailedStock)
	{
		this.detailedStock = detailedStock;
	}
}
