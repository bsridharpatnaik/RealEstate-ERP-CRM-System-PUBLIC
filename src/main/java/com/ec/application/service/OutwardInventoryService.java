package com.ec.application.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.OutwardInventoryWithDropdownValues;
import com.ec.application.model.OutwardInventory;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
@Service
public class OutwardInventoryService 
{
	@Autowired
	ProductService productService;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	StockService stockService;
	
	@Autowired
	OutwardInventoryRepo outwardInventoryRepo;
	
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	StockRepo stockRepo;
	
	public OutwardInventory createOutwardnventory(OutwardInventoryData oiData) throws Exception
	{
		OutwardInventory outwardnventory = new OutwardInventory();
		validateInputs(oiData);
		setFields(outwardnventory,oiData);
		Float closingStock = stockService.updateStock(oiData.getProductId(), "Default", oiData.getQuantity(), "outward");
		outwardnventory.setClosingStock(closingStock);
		return outwardInventoryRepo.save(outwardnventory);
	}
	public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(!outwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		Long oldProductId = outwardInventory.getProduct().getProductId();
		Float oldQuantity = stockRepo.findStockForProductAndWarehouse(outwardInventory.getProduct().getProductId(),outwardInventory.getWarehouse().getWarehouseName()).get(0).getQuantityInHand();
		validateInputs(iiData);
		setFields(outwardInventory,iiData);
		updateStock(oldProductId,iiData.getProductId(),outwardInventory,iiData.getQuantity(),oldQuantity);
		return outwardInventoryRepo.save(outwardInventory);
		
	}
	private void updateStock(Long oldProductId,Long newProductId,OutwardInventory outwardInventory , Float  quantity, Float oldQuantity) throws Exception 
	{
		if(oldProductId.equals(newProductId)==false)
		{
			Float closingStock = stockService.updateStock(oldProductId, "Default",oldQuantity , "inward");
			closingStock = stockService.updateStock(newProductId, "Default",quantity , "inward");
			outwardInventory.setClosingStock(closingStock);
		}
		else if(oldProductId.equals(newProductId) && quantity>oldQuantity)
		{
			Float diffInStock =  quantity - oldQuantity;
			Float currentStock = stockRepo.findStockForProductAndWarehouse(newProductId,outwardInventory.getWarehouse().getWarehouseName()).get(0).getQuantityInHand();
			
			if(diffInStock>currentStock)
				throw new Exception("Stock cannot be updated as available stock is less than difference requested in stock");
			
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "outward");
			outwardInventory.setClosingStock(closingStock);
			
			
		}
		else if(oldProductId.equals(newProductId) && quantity<oldQuantity)
		{
			Float diffInStock = quantity - oldQuantity;
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "inward");
			outwardInventory.setClosingStock(closingStock);
		}
		
		
	}
	private void setFields(OutwardInventory outwardnventory, OutwardInventoryData oiData) throws Exception 
	{
		outwardnventory.setDate(oiData.getDate());
		outwardnventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocation()).get());
		outwardnventory.setProduct(productService.findSingleProduct(oiData.getProductId()));
		outwardnventory.setPurpose(oiData.getPurpose());
		outwardnventory.setQuantity(oiData.getQuantity());
		outwardnventory.setSlipNo(oiData.getSlipNo());
		//outwardnventory.setUnloadingArea(unloadingAreaRepo.findById(oiData.getUnloadingAreaId()).get());
		//outwardnventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
	}

	private void validateInputs(OutwardInventoryData oiData) throws Exception 
	{
		if(oiData.getProductId() == null || oiData.getUnloadingAreaId()==null || oiData.getContractorId()==null || oiData.getUsageLocation()==null)
			throw new Exception("Required field missing");
		
		if(!productRepo.existsById(oiData.getProductId()))
				throw new Exception("Product with ID not found");
		//if(!contractorRepo.existsById(oiData.getContractorId()))
		//	throw new Exception("Vendor with ID not found");
		//if(!unloadingAreaRepo.existsById(oiData.getUnloadingAreaId()))
		//	throw new Exception("Unloading Area with ID not found");
		if(!locationRepo.existsById(oiData.getUsageLocation()))
			throw new Exception("Usage Location Not Found");
		if(oiData.getQuantity()<=0)
			throw new Exception("Quantity have to be greater the zero");
		
		//Modify this for multiple warehouses
		//if(stockRepo.findStockForProductAndWarehouse(oiData.getProductId(),warehouseName).get(0).getQuantityInHand() < oiData.getQuantity())
			//throw new Exception("Requested Quantity should not be greater than available quantity");
	}
	public OutwardInventoryWithDropdownValues findAll(Pageable pageable) 
	{
		OutwardInventoryWithDropdownValues inwardInventoryWithDropdownValues = new OutwardInventoryWithDropdownValues();
		inwardInventoryWithDropdownValues.setMorDropdown(populateDropdownService.fetchData("outward"));
		inwardInventoryWithDropdownValues.setOutwardInventory(outwardInventoryRepo.findAll(pageable));
		return inwardInventoryWithDropdownValues;
	}
	public OutwardInventory findOutwardnventory(Long id) throws Exception 
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(outwardInventoryOpt.isPresent()==false)
			throw new Exception("Outward inventory with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		return outwardInventory;
	}
	public void deleteOutwardnventory(Long id) throws Exception 
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(!outwardInventoryOpt.isPresent())
			throw new Exception("Outward Inventory with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		updateStockBeforeDelete(outwardInventory);
		outwardInventoryRepo.softDeleteById(id);
	}
	private void updateStockBeforeDelete(OutwardInventory outwardInventory) throws Exception 
	{
		Float stock = outwardInventory.getQuantity();
		stockService.updateStock(outwardInventory.getProduct().getProductId(), "Default", stock, "inward");
	}

}
