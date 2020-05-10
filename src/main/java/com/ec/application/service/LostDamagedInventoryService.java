package com.ec.application.service;

import java.text.ParseException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.CreateLostOrDamagedInventoryData;
import com.ec.application.data.LostDamagedReturnData;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.repository.LostDamagedInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.InwardInventorySpecification;
import com.ec.common.Filters.LostDamagedInventorySpecification;

@Service
public class LostDamagedInventoryService 
{
	@Autowired
	LostDamagedInventoryRepo lostDamagedInventoryRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	ProductService productService;
	
	@Autowired
	StockService stockService;
	
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	public LostDamagedInventory createData(CreateLostOrDamagedInventoryData payload) throws Exception
	{
		LostDamagedInventory lostDamagedInventory = new LostDamagedInventory();
		validatePayload(payload);
		populateData(lostDamagedInventory,payload);
		Float closingStock = adjustStock(payload);
		lostDamagedInventory.setClosingStock(closingStock);
		return lostDamagedInventoryRepo.save(lostDamagedInventory);
	}
	
	public LostDamagedReturnData findFiilteredostDamagedList(FilterDataList filterDataList, Pageable pageable) throws ParseException
	{
		Specification<LostDamagedInventory> spec = LostDamagedInventorySpecification.getSpecification(filterDataList);
		
		LostDamagedReturnData lostDamagedReturnData = new LostDamagedReturnData();
		if(spec!=null) lostDamagedReturnData.setLostDamagedInventories(lostDamagedInventoryRepo.findAll(spec,pageable));
		else lostDamagedReturnData.setLostDamagedInventories(lostDamagedInventoryRepo.findAll(pageable));
		lostDamagedReturnData.setLdDropdown(populateDropdownService.fetchData("lostdamaged"));
		return lostDamagedReturnData;
	}
	
	private Float adjustStock(CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		String warehouseName = warehouseRepo.findById(payload.getWarehouseId()).get().getWarehouseName();
		return stockService.updateStock(payload.getProductId(),warehouseName , payload.getQuantity(), "outward");
	}
	private void populateData(LostDamagedInventory lostDamagedInventory, CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		lostDamagedInventory.setLocationOfTheft(payload.getTheftLocation());
		lostDamagedInventory.setQuantity(payload.getQuantity());
		lostDamagedInventory.setProduct(productService.findSingleProduct(payload.getProductId()));
		lostDamagedInventory.setDate(payload.getDate());
		lostDamagedInventory.setWarehouse(warehouseRepo.findById(payload.getWarehouseId()).get());
	}
	
	public LostDamagedInventory UpdateData(CreateLostOrDamagedInventoryData payload,Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
		validatePayload(payload);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		populateData(lostDamagedInventory,payload);
		//UpdateStockBeforeUpdate(oldProductId,oldQuantity,lostDamagedInventory);
		return lostDamagedInventoryRepo.save(lostDamagedInventory);	
	}
	
	private void validatePayload(CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		if(!productRepo.findById(payload.getProductId()).isPresent())
			throw new Exception("Invalid Product ID");
		if(!warehouseRepo.findById(payload.getWarehouseId()).isPresent())
			throw new Exception("Invalid warehouse ID");
		if(payload.getQuantity()<=0)
			throw new Exception("Quantity should be greater than zero");
		
	}
/*
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
	
	*/
	public LostDamagedInventory findById(Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Lost Damaged Inventory by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		return lostDamagedInventory;	
	}
	public void DeleteData(Long id) throws Exception
	{
		Optional<LostDamagedInventory> lostDamagedInventoryOpt = lostDamagedInventoryRepo.findById(id);
		if(!lostDamagedInventoryOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		LostDamagedInventory lostDamagedInventory = lostDamagedInventoryOpt.get();
		AdjustStockBeforeDelete(lostDamagedInventory);
		lostDamagedInventoryRepo.softDeleteById(id);
	}
	private void AdjustStockBeforeDelete(LostDamagedInventory lostDamagedInventory) throws Exception 
	{
		stockService.updateStock(lostDamagedInventory.getProduct().getProductId(),lostDamagedInventory.getWarehouse().getWarehouseName() , lostDamagedInventory.getQuantity(), "inward");
	}

	public Page<LostDamagedInventory> findAll(Pageable pageable) {
		Page<LostDamagedInventory> allLODInv = lostDamagedInventoryRepo.findAll(pageable);
		return allLODInv;
	}

}
