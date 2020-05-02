package com.ec.application.service;

import java.text.ParseException;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnInwardInventoryData;
import com.ec.application.data.ReturnOutwardInventoryData;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.OutwardInventorySpecification;
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
	
	@Autowired
	ContractorRepo contractorRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;
	
	@Autowired
	InwardInventoryService iiService;
	
	@Transactional
	public OutwardInventory createOutwardnventory(OutwardInventoryData oiData) throws Exception
	{
		OutwardInventory outwardInventory = new OutwardInventory();
		validateInputs(oiData);
		setFields(outwardInventory,oiData);
		//updateStockForCreateInwardInventory(inwardInventory);
		return outwardInventoryRepo.save(outwardInventory);
	}

	public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(!outwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		validateInputs(iiData);
		setFields(outwardInventory,iiData);
		//updateStock(oldProductId,iiData.getProductId(),outwardInventory,iiData.getQuantity(),oldQuantity);
		return outwardInventoryRepo.save(outwardInventory);
		
	}
	
	private void setFields(OutwardInventory outwardInventory, OutwardInventoryData oiData) 
	{
		Warehouse warehouse = warehouseRepo.findById(oiData.getWarehouseId()).get();
		outwardInventory.setAdditionalInfo(oiData.getAdditionalInfo());
		outwardInventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
		outwardInventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocationId()).get());
		outwardInventory.setWarehouse(warehouse);;
		outwardInventory.setDate(oiData.getDate());
		outwardInventory.setPurpose(oiData.getPurpose());
		outwardInventory.setSlipNo(oiData.getPurpose());
		outwardInventory.setInwardOutwardList(iiService.fetchInwardOutwardList(oiData.getProductWithQuantities(),warehouse));	
	}

	private void validateInputs(OutwardInventoryData oiData) throws Exception 
	{
		if(!locationRepo.existsById(oiData.getUsageLocationId()))
			throw new Exception("Usage Location not found.");
		if(!contractorRepo.existsById(oiData.getContractorId()))
			throw new Exception("Contractor not found.");
		if(!warehouseRepo.existsById(oiData.getWarehouseId()))
			throw new Exception("Contractor not found.");
		for(ProductWithQuantity productWithQuantity : oiData.getProductWithQuantities())
		{
			if(!productRepo.existsById(productWithQuantity.getProductId()))
				throw new Exception("Product not found.");
			if(productWithQuantity.getQuantity()<=0)
				throw new Exception("Quantity should be greater than zero");
		}
	}
	
	public OutwardInventory findOutwardnventory(Long id) throws Exception 
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(outwardInventoryOpt.isPresent()==false)
			throw new Exception("Outward inventory with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		return outwardInventory;
	}

	public ReturnOutwardInventoryData fetchOutwardnventory(FilterDataList filterDataList, Pageable pageable) throws ParseException 
	{
		ReturnOutwardInventoryData returnOutwardInventoryData = new ReturnOutwardInventoryData();
		
		Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
		
		if(spec!=null) returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(spec,pageable));
		else returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(pageable));
		returnOutwardInventoryData.setIiDropdown(populateDropdownService.fetchData("outward"));
		return returnOutwardInventoryData;
	}

	@Transactional
	public void deleteOutwardInventoryById(Long id) throws Exception 
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(!outwardInventoryOpt.isPresent())
			throw new Exception("Outward Inventory with ID not found");
		OutwardInventory inwardInventory = outwardInventoryOpt.get();
		//updateStockBeforeDelete(inwardInventory);
		outwardInventoryRepo.softDeleteById(id);
	}
	
	
	/*
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
*/
}
