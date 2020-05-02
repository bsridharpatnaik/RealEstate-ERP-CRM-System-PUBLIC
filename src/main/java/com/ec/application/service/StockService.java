package com.ec.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.data.CurrentStockRequest;
import com.ec.application.data.SingleStockInfo;
import com.ec.application.data.StockInformation;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.Product;
import com.ec.application.model.Stock;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockSpecification;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

@Service
public class StockService 
{
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;

	public StockInformation findStockForAll(FilterDataList filterDataList,Pageable pageable)
	{
		Specification<Stock> spec = StockSpecification.getSpecification(filterDataList);
		List<Stock> allStocks = new ArrayList<Stock>();
		if(spec!=null) allStocks = stockRepo.findAll(spec);
		else allStocks = stockRepo.findAll();
		
		return fetchStockInformation(allStocks,pageable);
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
	
	public StockInformation fetchStockInformation(List<Stock> allStocks,Pageable pageable)
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
		stockInformation.setStockInformation(convertListStockToPages(stockInformationsList,pageable));
		return stockInformation;
	}

	private Page<SingleStockInfo> convertListStockToPages(List<SingleStockInfo> stockInformationsList,
			Pageable pageable) 
	{
		  int start =  (int) pageable.getOffset();
	      int end = (start + pageable.getPageSize()) > stockInformationsList.size() ? stockInformationsList.size() : (start + pageable.getPageSize());
	      return new PageImpl<SingleStockInfo>(stockInformationsList.subList(start, end), pageable, stockInformationsList.size());
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

	public HashMap<Long, Float> modifyStockBeforeUpdate(String oldWarehouseName, String newWarehousename, HashMap<Long, Float> oldStock,
			HashMap<Long, Float> newStock, String transType) throws Exception 
	{
		HashMap<Long, Float> differenceInStock = new HashMap<Long, Float>();
		MapDifference<Long , Float> diff = Maps.difference(oldStock, newStock);
		Set<Long> keysOnlyInOld = diff.entriesOnlyOnLeft().keySet();
		Set<Long> keysOnlyInNew = diff.entriesOnlyOnRight().keySet();
		differenceInStock = findStockForCommonIds(oldStock,newStock);
		HashMap<Long, Float> closingStocks = updateStockForMaps(differenceInStock,keysOnlyInOld,keysOnlyInNew,oldStock,newStock,oldWarehouseName,newWarehousename,transType);
		return closingStocks;
	}
	private HashMap<Long, Float> updateStockForMaps(HashMap<Long, Float> differenceInStock, Set<Long> keysOnlyInOld,
			Set<Long> keysOnlyInNew, HashMap<Long, Float> oldStock, HashMap<Long, Float> newStock,String oldWarehouseName,String newWarehousename,String transType ) throws Exception 
	{
		HashMap<Long, Float> closingStocks = new HashMap<Long, Float>();
		for(Long onlyInOldProductId : keysOnlyInOld)
		{
			String updateType = transType.equalsIgnoreCase("inward")?"outward":"inward";
			Float closingStock = updateStock(onlyInOldProductId, oldWarehouseName, oldStock.get(onlyInOldProductId), updateType);
			closingStocks.put(onlyInOldProductId, closingStock);
		}
		for(Long onlyInNewProductId : keysOnlyInNew)
		{
			String updateType = transType.equalsIgnoreCase("inward")?"inward":"outward";
			Float closingStock = updateStock(onlyInNewProductId, newWarehousename, newStock.get(onlyInNewProductId), updateType);
			closingStocks.put(onlyInNewProductId, closingStock);
		}
		Iterator diffInStockIterator = differenceInStock.entrySet().iterator();
		while (diffInStockIterator.hasNext()) 
		{ 
            Map.Entry mapElementOld = (Map.Entry)diffInStockIterator.next(); 
            if(newWarehousename.equalsIgnoreCase(oldWarehouseName))
            {
            	Float closingStock = updateStock((long)mapElementOld.getKey(), newWarehousename, (Float) mapElementOld.getValue(), transType);
            	closingStocks.put((long)mapElementOld.getKey(), closingStock);
            }
            else
            {
            	String updateType = transType.equalsIgnoreCase("inward")?"outward":"inward";
            	Long productId = (long)mapElementOld.getKey();
            	updateStock(productId, oldWarehouseName, oldStock.get(productId), updateType);
            	Float closingStock = updateStock(productId, newWarehousename, oldStock.get(productId), transType);
            	closingStocks.put(productId, oldStock.get(productId));
            }
		}
		return closingStocks; 
		
	}

	private HashMap<Long, Float> findStockForCommonIds(HashMap<Long, Float> oldStock, HashMap<Long, Float> newStock) 
	{
		HashMap<Long, Float> differenceInStock = new HashMap<Long, Float>();
		
		Iterator oldStockIterator = oldStock.entrySet().iterator(); 
		while (oldStockIterator.hasNext()) 
		{ 
            Map.Entry mapElementOld = (Map.Entry)oldStockIterator.next(); 
            Long oldProductId = (Long) mapElementOld.getKey();
            Float oldQuantity = (Float) mapElementOld.getValue();
            Float newQuantity = newStock.get(oldProductId);
            if(newQuantity!=null)
                differenceInStock.put(oldProductId, newQuantity - oldQuantity);
        }
		return differenceInStock;
	}

	public Float findStockForProductWarehouse(Long productId,Long warehouseId) 
	{
		Float currentStock = stockRepo.getCurrentStockForProductWarehouse(productId,warehouseId);
		return currentStock;
	}

	public InwardInventory updateClosingStockToSet(InwardInventory inwardInventory, HashMap<Long, Float> closingStocks) 
	{
		Set<InwardOutwardList> inwardOutwardSet = new HashSet<InwardOutwardList>();
		inwardOutwardSet = inwardInventory.getInwardOutwardList();
		inwardOutwardSet = updateClosingStockToInwardOutwardSet(inwardOutwardSet,closingStocks);
		inwardInventory.setInwardOutwardList(inwardOutwardSet);
		return inwardInventory;
	}

	private Set<InwardOutwardList> updateClosingStockToInwardOutwardSet(Set<InwardOutwardList> inwardOutwardSet,
			HashMap<Long, Float> closingStocks) 
	{
		Iterator closingStockIterator = closingStocks.entrySet().iterator();
		LinkedList<InwardOutwardList> inwardOutwardList = new LinkedList<InwardOutwardList>();
        inwardOutwardList.addAll(inwardOutwardSet);
		while (closingStockIterator.hasNext()) 
		{ 
            Map.Entry mapElementOld = (Map.Entry)closingStockIterator.next();
            Long productId = (Long) mapElementOld.getKey();
            Float closingStock = (Float) mapElementOld.getValue();
            
            for(int ctr=0;ctr<inwardOutwardList.size();ctr++)
            {
            	Long tempProductId = inwardOutwardList.get(ctr).getProduct().getProductId();
            	if(tempProductId.equals(productId))
            		inwardOutwardList.get(ctr).setClosingStock(closingStock);
            }
		}
		inwardOutwardSet.clear();
        inwardOutwardSet.addAll(inwardOutwardList);
		return inwardOutwardSet;
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
			ProductIdAndStockProjection productsWarehouseStockProjection = new ProductIdAndStockProjection(productId,(float)0);
			stockInfo.add(productsWarehouseStockProjection);
		}
		return stockInfo;
	}
}
