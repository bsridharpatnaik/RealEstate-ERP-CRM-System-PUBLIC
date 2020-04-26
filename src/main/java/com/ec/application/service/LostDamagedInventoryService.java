package com.ec.application.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.CreateLostOrDamagedInventoryData;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.Product;
import com.ec.application.repository.LostDamagedInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;

@Service
public class LostDamagedInventoryService 
{
	@Autowired
	LostDamagedInventoryRepo LostDamagedInventoryRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	ProductService productService;
	
	@Autowired
	StockService stockService;
	
	@Autowired
	StockRepo stockRepo;
	
	public LostDamagedInventory createData(CreateLostOrDamagedInventoryData payload) throws Exception
	{
		LostDamagedInventory lostDamagedInventory = new LostDamagedInventory();
		populateData(lostDamagedInventory,payload);
		Float closingStock = adjustStock(payload);
		lostDamagedInventory.setClosingStock(closingStock);
		return LostDamagedInventoryRepo.save(lostDamagedInventory);
	}
	
	private Float adjustStock(CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		return stockService.updateStock(payload.getProductId(), "Default", payload.getQuantity(), "outward");
	}
	private void populateData(LostDamagedInventory lostDamagedInventory, CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		lostDamagedInventory.setLocationOfTheft(payload.getTheftLocation());
		lostDamagedInventory.setQuantity(payload.getQuantity());
		lostDamagedInventory.setProduct(productService.findSingleProduct(payload.getProductId()));
		lostDamagedInventory.setDate(payload.getDate());
	}
	
	public LostDamagedInventory UpdateData(CreateLostOrDamagedInventoryData payload,Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = LostDamagedInventoryRepo.findById(id);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		
		Long oldProductId = lostDamagedInventory.getProduct().getProductId();
		Float oldQuantity = lostDamagedInventory.getQuantity();
		populateData(lostDamagedInventory,payload);
		UpdateStockBeforeUpdate(oldProductId,oldQuantity,lostDamagedInventory);
		return LostDamagedInventoryRepo.save(lostDamagedInventory);	
	}
	
	private void UpdateStockBeforeUpdate(Long oldProductId, Float oldQuantity, LostDamagedInventory lostDamagedInventory) throws Exception 
	{
		Long newProductId = lostDamagedInventory.getProduct().getProductId();
		String warehouseName = lostDamagedInventory.getWarehouse().getWarehouseName();
		Float quantity = lostDamagedInventory.getQuantity();
		if(oldProductId.equals(newProductId)==false)
		{
			Float closingStock = stockService.updateStock(oldProductId, "Default",oldQuantity , "outward");
			closingStock = stockService.updateStock(newProductId, "Default",quantity , "inward");
			lostDamagedInventory.setClosingStock(closingStock);
		}
		else if(oldProductId.equals(newProductId) && quantity>oldQuantity)
		{
			Float diffInStock = quantity - oldQuantity;
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "inward");
			lostDamagedInventory.setClosingStock(closingStock);
		}
		else if(oldProductId.equals(newProductId) && quantity<oldQuantity)
		{
			Float diffInStock =  oldQuantity - quantity ;
			
			//update this in case of multi warehouse
			Float currentStock = stockRepo.findStockForProductAndWarehouse(newProductId,warehouseName).get(0).getQuantityInHand();
			
			if(diffInStock>currentStock)
				throw new Exception("Stock cannot be updated as available stock is less than difference requested in stock");
			
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "outward");
			lostDamagedInventory.setClosingStock(closingStock);
		}
		
	}
	public LostDamagedInventory findById(Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = LostDamagedInventoryRepo.findById(id);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Lost Damaged Inventory by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		return lostDamagedInventory;
		
	}
	public void DeleteData(Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = LostDamagedInventoryRepo.findById(id);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		AdjustStockBeforeDelete(lostDamagedInventory);
		LostDamagedInventoryRepo.softDeleteById(id);
	}
	private void AdjustStockBeforeDelete(LostDamagedInventory lostDamagedInventory) throws Exception 
	{
		stockService.updateStock(lostDamagedInventory.getProduct().getProductId(), "Default", lostDamagedInventory.getQuantity(), "inward");
	}

	public Page<LostDamagedInventory> findAll(Pageable pageable) {
		Page<LostDamagedInventory> allLODInv = LostDamagedInventoryRepo.findAll(pageable);
		return allLODInv;
	}

}
