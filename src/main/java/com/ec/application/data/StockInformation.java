package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

public class StockInformation 
{
	Page<SingleStockInfo> stockInformation;

	public Page<SingleStockInfo> getStockInformation() {
		return stockInformation;
	}

	public void setStockInformation(Page<SingleStockInfo> stockInformation) {
		this.stockInformation = stockInformation;
	}

	
}
