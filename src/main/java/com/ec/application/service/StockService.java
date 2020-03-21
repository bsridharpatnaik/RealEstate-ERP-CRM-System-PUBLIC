package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.BasicEntities.Product;
import com.ec.application.model.Stock.Stock;
import com.ec.application.model.Stock.Warehouse;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;

@Service
public class StockService 
{
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;

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
	
	public Float updateStock(Long productId,String warehousename, Float quantity, String operation) throws Exception
	{
		Stock currentStock = findOrInsertStock(productId,warehousename);
	
		Float oldStock = currentStock.getQuantityInHand();
		Float newStock = (float) 0;
		switch(operation)
		{
		case "inward":
			newStock = oldStock+quantity;
			break;
		case "outward":
			newStock = oldStock - quantity;
		} 
		if(newStock<0)
			throw new Exception("Stock cannot be Negative");
		else 
		{
			currentStock.setQuantityInHand(newStock);
			stockRepo.save(currentStock);
			return newStock;
		}
	}

	private Stock findOrInsertStock(Long productId,String warehousename) throws Exception 
	{
		Optional<Product> productOpt = productRepo.findById(productId);
		List<Warehouse> warehouseOpt = warehouseRepo.findByName(warehousename);
		
		if(!productOpt.isPresent() || warehouseOpt.size()!=1)
			throw new Exception("Product of warehouse not found");
		
		Product product = productOpt.get();
		Warehouse warehouse = warehouseOpt.get(0);
		List<Stock> stocks = stockRepo.findByIdName(productId,warehousename);
		if(stocks.size()==0)
		{
			Stock stock = new Stock(product,warehouse,(float) 0.0);
			return stockRepo.save(stock);
		}
		else
		{
			return stocks.get(0);
		}
	}
}
