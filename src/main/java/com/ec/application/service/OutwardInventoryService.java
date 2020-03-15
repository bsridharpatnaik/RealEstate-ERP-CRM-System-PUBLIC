package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.OutwardInventoryWithDropdownValues;
import com.ec.application.model.OutwardInventory;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.UnloadingAreaRepo;
import com.ec.application.repository.VendorRepo;

@Service
public class OutwardInventoryService 
{
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
	OutwardInventoryRepo outwardInventoryRepo;
	
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	ContractorRepo contractorRepo;
	
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
	
	private void setFields(OutwardInventory outwardnventory, OutwardInventoryData oiData) 
	{
		outwardnventory.setDate(oiData.getDate());
		outwardnventory.setLocation(locationRepo.findById(oiData.getUsageLocation()).get());
		outwardnventory.setProduct(productRepo.findById(oiData.getProductId()).get());
		outwardnventory.setPurpose(oiData.getPurpose());
		outwardnventory.setQuantity(oiData.getQuantity());
		outwardnventory.setSlipNo(oiData.getSlipNo());
		outwardnventory.setUnloadingArea(unloadingAreaRepo.findById(oiData.getUnloadingAreaId()).get());
		outwardnventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
	}

	private void validateInputs(OutwardInventoryData oiData) throws Exception 
	{
		if(oiData.getProductId() == null || oiData.getUnloadingAreaId()==null || oiData.getContractorId()==null || oiData.getUsageLocation()==null)
			throw new Exception("Required field missing");
		
		if(!productRepo.existsById(oiData.getProductId()))
				throw new Exception("Product with ID not found");
		if(!contractorRepo.existsById(oiData.getContractorId()))
			throw new Exception("Vendor with ID not found");
		if(!unloadingAreaRepo.existsById(oiData.getUnloadingAreaId()))
			throw new Exception("Unloading Area with ID not found");
		if(!locationRepo.existsById(oiData.getUsageLocation()))
			throw new Exception("Usage Location Not Found");
		if(oiData.getQuantity()<=0)
			throw new Exception("Quantity have to be greater the zero");
		
		//Modify this for multiple warehouses
		if(stockRepo.findStockForProductAsList(oiData.getProductId()).get(0).getQuantityInHand() < oiData.getQuantity())
			throw new Exception("Requested Quantity should not be greater than available quantity");
	}
	public OutwardInventoryWithDropdownValues findAll(Pageable pageable) 
	{
		OutwardInventoryWithDropdownValues inwardInventoryWithDropdownValues = new OutwardInventoryWithDropdownValues();
		inwardInventoryWithDropdownValues.setMorDropdown(populateDropdownService.fetchData("outward"));
		inwardInventoryWithDropdownValues.setOutwardInventory(outwardInventoryRepo.findAll(pageable));
		return inwardInventoryWithDropdownValues;
	}

}
