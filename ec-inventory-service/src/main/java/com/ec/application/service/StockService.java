package com.ec.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.data.CurrentStockRequest;
import com.ec.application.data.SingleStockInfo;
import com.ec.application.data.StockInformation;
import com.ec.application.data.StockPercentData;
import com.ec.application.model.Product;
import com.ec.application.model.Stock;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockSpecification;
import java.text.DecimalFormat;
@Service
public class StockService 
{
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	ProductService productService;
	
	@Autowired
	WarehouseRepo warehouseRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	WarehouseService warehouseService;
	
	@Autowired
	InventoryNotificationService inventoryNotificationService;

	public StockInformation findStockForAll(FilterDataList filterDataList,Pageable pageable) throws Exception
	{
		Specification<Stock> spec = StockSpecification.getSpecification(filterDataList);
		List<Stock> allStocks = new ArrayList<Stock>();
		if(spec!=null) allStocks = stockRepo.findAll(spec);
		else allStocks = stockRepo.findAll();
		
		return fetchStockInformation(allStocks,pageable);
	}
	
	public Double updateStock(Long productId,String warehousename, Double quantity, String operation) throws Exception
	{
		Stock currentStock = findOrInsertStock(productId,warehousename);
		System.out.println("Current Stock - "+ currentStock.getQuantityInHand());
		System.out.println("New Quantity - "+ quantity);
		Double oldStock = currentStock.getQuantityInHand();
		Double newStock = (double) 0;
		switch(operation)
		{
			case "inward":
				newStock = oldStock+quantity;
				break;
			case "outward":
				newStock = oldStock - quantity;
		} 
		System.out.println("New Stock - "+newStock);
		if(newStock<0)
			throw new Exception("Stock cannot be Negative");
		else 
		{
			currentStock.setQuantityInHand(newStock);
			stockRepo.save(currentStock);
			inventoryNotificationService.checkStockAndPushLowStockNotification(currentStock.getProduct());
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
			Stock stock = new Stock(product,warehouse, 0.0);
			return stockRepo.save(stock);
		}
		else
		{
			return stocks.get(0);
		}
	}
	
	public StockInformation fetchStockInformation(List<Stock> allStocks,Pageable pageable) throws Exception
	{
		StockInformation stockInformation = new StockInformation();
		List<SingleStockInfo> stockInformationsList = new ArrayList<SingleStockInfo>();
		List<Long> uniqueProductIds = fetchUniqueProductIds(allStocks);
		for(Long productId : uniqueProductIds)
		{
			DecimalFormat df = new DecimalFormat("###.#");
			Product product = productService.findSingleProduct(productId);
			SingleStockInfo singleStockInfo = new SingleStockInfo();
			singleStockInfo.setProductId(productId);
			singleStockInfo.setProductName(product.getProductName());
			singleStockInfo.setCategoryName(product.getCategory().getCategoryName());
			singleStockInfo.setTotalQuantityInHand(df.format(stockRepo.getTotalStockForProduct(productId)));
			singleStockInfo.setDetailedStock(findStockForProductAsList(productId,allStocks));
			stockInformationsList.add(singleStockInfo);
		}
		stockInformation.setStockInformation(convertListStockToPages(stockInformationsList,pageable));
		stockInformation.setStockDropdownValues(populateDropdownService.fetchData("stock"));
		return stockInformation;
	}

	private Page<SingleStockInfo> convertListStockToPages(List<SingleStockInfo> stockInformationsList,
			Pageable pageable) 
	{
		 int start =  (int) pageable.getOffset();
	     int end = (start + pageable.getPageSize()) > stockInformationsList.size() ? stockInformationsList.size() : (start + pageable.getPageSize());
	     stockInformationsList = sortStockInformationsList(stockInformationsList,pageable.getSort());
	     return new PageImpl<SingleStockInfo>(stockInformationsList.subList(start, end), pageable, stockInformationsList.size());
	}

	private List<SingleStockInfo> sortStockInformationsList(List<SingleStockInfo> stockInformationsList, Sort sort) 
	{
		try
		{
			String[] sortBy = sort.toString().split(":");
			String field = sortBy[0].trim();
			String order = sortBy[1].trim();
			
			switch(field)
			{
				case "productId":
					if(order.toLowerCase().contains("desc"))
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductId, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
					else
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductId, Comparator.nullsFirst(Comparator.naturalOrder())));
					break;
				case "productName":
					if(order.toLowerCase().contains("desc"))
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductName, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
					else
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductName, Comparator.nullsFirst(Comparator.naturalOrder())));
					break;
				case "categoryName":
					if(order.toLowerCase().contains("desc"))
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getCategoryName, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
					else
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getCategoryName, Comparator.nullsFirst(Comparator.naturalOrder())));
					break;
				case "totalQuantityInHand":
					if(order.toLowerCase().contains("desc"))
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getTotalQuantityInHand, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
					else
						stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getTotalQuantityInHand, Comparator.nullsFirst(Comparator.naturalOrder())));
					break;
			}
			return stockInformationsList;
		}
		catch(Exception e)
		{
			System.out.println("Sorting failed");
			return stockInformationsList;
		}
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

	public Double findStockForProductWarehouse(Long productId,Long warehouseId) 
	{
		Double currentStock = stockRepo.getCurrentStockForProductWarehouse(productId,warehouseId);
		return currentStock;
	}

	public Double findTotalStockForProduct(Long productId) 
	{
		Double currentStock = stockRepo.getCurrentTotalStockForProduct(productId);
		return currentStock;
	}
	
	public List<ProductIdAndStockProjection> findStockForProductListWarehouse(CurrentStockRequest currentStockRequest) 
	{
		List<Long> productIds = currentStockRequest.getProductIds();
		Long warehouseId = currentStockRequest.getWarehouseId();
		List<ProductIdAndStockProjection> stockInfo = stockRepo.getCurrentStockForProductListWarehouse(productIds,warehouseId);
		List<Long> returnedProductIds = stockInfo.stream().map(ProductIdAndStockProjection::getProductId).collect(Collectors.toList());
		productIds.removeAll(returnedProductIds);
		for(Long productId:productIds)
		{
			ProductIdAndStockProjection productsWarehouseStockProjection = new ProductIdAndStockProjection(productId,(double)0);
			stockInfo.add(productsWarehouseStockProjection);
		}
		return stockInfo;
	}

	public List<StockPercentData> fetchStockPercent() 
	{
		return stockRepo.getCurrentStockPercent();
	}
}
