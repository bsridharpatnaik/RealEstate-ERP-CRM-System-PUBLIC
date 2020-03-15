package com.ec.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Stock.Stock;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;

@Service
public class StockService 
{
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	//@Autowired
	//WarehouseRepo warehouseRepo;

	public Page<Stock> findStockForAll(Pageable pageable)
	{
		return stockRepo.findAll(pageable);
	}
	
	public Page<Stock> findStockForCategory(Pageable pageable,Long categoryID)
	{
		return stockRepo.findStockForCategory(pageable,categoryID);
	}
	
	public Page<Stock> findStockForProduct(Pageable pageable,Long productId)
	{
		return stockRepo.findStockForProduct(pageable,productId);
	}
	
	public void updateStock(Long productId,String warehousename, Float quantity, String operation)
	{
		//Stock currentStock = findOrInsertStock(productId,warehousename);
		
	}
/*
	private Stock findOrInsertStock(Long productId,String warehousename) 
	{
		
		List<Stock> stocks = stockRepo.findByIdName(productId,warehousename);
		if(stocks.size()==0)
		{
			//Stock stock = new Stock(productId,warehousename,0);
			
		}
		
	}*/
}
