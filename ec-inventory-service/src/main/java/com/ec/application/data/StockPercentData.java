package com.ec.application.data;

import java.util.Date;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.CustomDoubleSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class StockPercentData
{
	Long productId;
	String productName;
	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double stockPercent;
	Date updated;

	public StockPercentData(Long productId, String productName, Date updated, Double stockPercent)
	{
		super();
		this.productId = productId;
		this.productName = productName;
		this.stockPercent = stockPercent;
		this.updated = updated;
	}

	public Date getUpdated()
	{
		return updated;
	}

	public void setUpdated(Date updated)
	{
		this.updated = updated;
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

	@JsonSerialize(using = CustomDoubleSerializer.class)
	public Double getStockPercent()
	{
		return stockPercent;
	}

	public void setStockPercent(Double stockPercent)
	{
		this.stockPercent = stockPercent;
	}

}
