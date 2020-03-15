package com.ec.application.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.InwardInventoryWithDropdownValues;
import com.ec.application.model.InwardInventory;
import com.ec.application.repository.InwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.UnloadingAreaRepo;
import com.ec.application.repository.VendorRepo;

@Service
public class InwardInventoryService 
{

	@Autowired
	InwardInventoryRepo inwInvRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	VendorRepo vendorRepo;
	
	@Autowired
	UnloadingAreaRepo unloadingAreaRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	StockService stockService;
	
	@Autowired
	StockRepo stockRepo;
	public InwardInventory createInwardnventory(InwardInventoryData iiData) throws Exception
	{
		InwardInventory inwardInventory = new InwardInventory();
		validateInputs(iiData);
		setFields(inwardInventory,iiData);
		Float closingStock = stockService.updateStock(iiData.getProductId(), "Default", iiData.getQuantity(), "inward");
		inwardInventory.setClosingStock(closingStock);
		return inwInvRepo.save(inwardInventory);
		
	}

	public InwardInventory findInwardnventory(Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwInvRepo.findById(id);
		if(inwardInventoryOpt.isPresent()==false)
			throw new Exception("Inward inventory with ID not found");
		return inwardInventoryOpt.get();
		
	}
	public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwInvRepo.findById(id);
		if(!inwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		Long oldProductId = inwardInventory.getProduct().getProductId();
		Float oldQuantity = stockRepo.findStockForProductAsList(inwardInventory.getProduct().getProductId()).get(0).getQuantityInHand();
		validateInputs(iiData);
		setFields(inwardInventory,iiData);
		updateStock(oldProductId,iiData.getProductId(),inwardInventory,iiData.getQuantity(),oldQuantity);
		return inwInvRepo.save(inwardInventory);
		
	}
	private void updateStock(Long oldProductId,Long newProductId,InwardInventory inwardInventory , Float  quantity, Float oldQuantity) throws Exception 
	{
		if(oldProductId.equals(newProductId)==false)
		{
			Float closingStock = stockService.updateStock(oldProductId, "Default",oldQuantity , "outward");
			closingStock = stockService.updateStock(newProductId, "Default",quantity , "inward");
			inwardInventory.setClosingStock(closingStock);
		}
		else if(oldProductId.equals(newProductId) && quantity>oldQuantity)
		{
			Float diffInStock = quantity - oldQuantity;
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "inward");
			inwardInventory.setClosingStock(closingStock);
		}
		else if(oldProductId.equals(newProductId) && quantity<oldQuantity)
		{
			Float diffInStock =  oldQuantity - quantity ;
			
			//update this in case of multi warehouse
			Float currentStock = stockRepo.findStockForProductAsList(newProductId).get(0).getQuantityInHand();
			
			if(diffInStock>currentStock)
				throw new Exception("Stock cannot be updated as available stock is less than difference requested in stock");
			
			Float closingStock = stockService.updateStock(newProductId, "Default", diffInStock, "outward");
			inwardInventory.setClosingStock(closingStock);
		}
		
		
	}

	private void setFields(InwardInventory inwardInventory, InwardInventoryData iiData) 
	{
		inwardInventory.setDate(iiData.getDate());
		inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
		inwardInventory.setProduct(productRepo.findById(iiData.getProductId()).get());
		inwardInventory.setQuantity(iiData.getQuantity());
		inwardInventory.setUnloadingArea(unloadingAreaRepo.findById(iiData.getUnloadingAreaId()).get());
		inwardInventory.setVehicleNo(iiData.getVehicleNo());
		inwardInventory.setVendor(vendorRepo.findById(iiData.getVendorId()).get());
		inwardInventory.setVendorSlipNo(iiData.getVendorSlipNo());
	}

	private void validateInputs(InwardInventoryData iiData) throws Exception 
	{
		if(iiData.getProductId() == null || iiData.getUnloadingAreaId()==null || iiData.getVendorId()==null)
			throw new Exception("Required field missing");
		
		if(!productRepo.existsById(iiData.getProductId()))
				throw new Exception("Product with ID not found");
		if(!vendorRepo.existsById(iiData.getVendorId()))
			throw new Exception("Vendor with ID not found");
		if(!unloadingAreaRepo.existsById(iiData.getUnloadingAreaId()))
			throw new Exception("Unloading Area with ID not found");
		if(iiData.getQuantity()<=0)
			throw new Exception("Quantity have to be greater the zero");
	}

	public InwardInventoryWithDropdownValues findAll(Pageable pageable) 
	{
		InwardInventoryWithDropdownValues inwardInventoryWithDropdownValues = new InwardInventoryWithDropdownValues();
		inwardInventoryWithDropdownValues.setMorDropdown(populateDropdownService.fetchData("inward"));
		inwardInventoryWithDropdownValues.setInwardInventory(inwInvRepo.findAll(pageable));
		return inwardInventoryWithDropdownValues;
	}
}
