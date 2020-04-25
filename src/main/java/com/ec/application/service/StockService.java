package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.SingleStockInfo;
import com.ec.application.data.StockInformation;
import com.ec.application.model.Product;
import com.ec.application.model.Stock;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockSpecification;

@Service
public class StockService 
{
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;

	public StockInformation findStockForAll(FilterDataList filterDataList)
	{
		Specification<Stock> spec = StockSpecification.getSpecification(filterDataList);
		List<Stock> allStocks = new ArrayList<Stock>();
		if(spec!=null) allStocks = stockRepo.findAll(spec);
		else allStocks = stockRepo.findAll();
		
		return fetchStockInformation(allStocks);
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
	
	public StockInformation fetchStockInformation(List<Stock> allStocks)
	{
		StockInformation stockInformation = new StockInformation();
		List<SingleStockInfo> stockInformationsList = new ArrayList<SingleStockInfo>();
		List<Long> uniqueProductIds = fetchUniqueProductIds(allStocks);
		for(Long productId : uniqueProductIds)
		{
			SingleStockInfo singleStockInfo = new SingleStockInfo();
			singleStockInfo.setProductId(productId);
			singleStockInfo.setTotalQuantityInHand(stockRepo.getTotalStockForProduct(productId));
			singleStockInfo.setDetailedStock(findStockForProductAsList(productId,allStocks));
			stockInformationsList.add(singleStockInfo);
		}
		stockInformation.setStockInformation(stockInformationsList);
		return stockInformation;
	}

	private List<Stock> findStockForProductAsList(Long productId, List<Stock> allStocks) 
	{
		List<Stock> stocks = allStocks.stream().filter(p -> p.getProduct().getProductId()==productId).collect(Collectors.toList());
		return stocks;
	}

	private List<Long> fetchUniqueProductIds(List<Stock> allStocks) 
	{
		return allStocks.stream()
                .map(p -> p.getProduct().getProductId()).distinct().collect(Collectors.toList());
	}
}
