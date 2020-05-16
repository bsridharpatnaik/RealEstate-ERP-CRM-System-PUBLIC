package com.ec.application.service;

import java.text.ParseException;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnOutwardInventoryData;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.UsageAreaRepo;
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
	
	@Autowired
	UsageAreaRepo usageAreaRepo;
	
	@Transactional
	public OutwardInventory createOutwardnventory(OutwardInventoryData oiData) throws Exception
	{
		OutwardInventory outwardInventory = new OutwardInventory();
		validateInputs(oiData);
		setFields(outwardInventory,oiData);
		updateStockForCreateOutwardInventory(outwardInventory);
		return outwardInventoryRepo.save(outwardInventory);
	}

	private void updateStockForCreateOutwardInventory(OutwardInventory outwardInventory) throws Exception 
	{
		Set<InwardOutwardList> productsWithQuantities = outwardInventory.getInwardOutwardList();
		String warehouseName = outwardInventory.getWarehouse().getWarehouseName();
		
		for(InwardOutwardList oiList : productsWithQuantities)
		{
			Long productId = oiList.getProduct().getProductId();
			Double quantity =  oiList.getQuantity();
			Double closingStock = stockService.updateStock(productId, warehouseName, quantity, "outward");
			oiList.setClosingStock(closingStock);
		}
	}

	@Transactional
	public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception
	{
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if(!outwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		validateInputs(iiData);
		updateStockBeforeDelete(outwardInventory);
		setFields(outwardInventory,iiData);
		updateStockForCreateOutwardInventory(outwardInventory);
		return outwardInventoryRepo.save(outwardInventory);
		
	}
	
	private void setFields(OutwardInventory outwardInventory, OutwardInventoryData oiData) 
	{
		Warehouse warehouse = warehouseRepo.findById(oiData.getWarehouseId()).get();
		outwardInventory.setAdditionalInfo(oiData.getAdditionalInfo());
		outwardInventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
		outwardInventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocationId()).get());
		outwardInventory.setUsageArea(usageAreaRepo.findById(oiData.getUsageAreaId()).get());
		outwardInventory.setWarehouse(warehouse);;
		outwardInventory.setDate(oiData.getDate());
		outwardInventory.setPurpose(oiData.getPurpose());
		outwardInventory.setSlipNo(oiData.getPurpose());
		outwardInventory.setInwardOutwardList(iiService.fetchInwardOutwardList(oiData.getProductWithQuantities(),warehouse));	
		outwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(oiData.getFileInformations()));
	}

	private void validateInputs(OutwardInventoryData oiData) throws Exception 
	{
		if(!locationRepo.existsById(oiData.getUsageLocationId()))
			throw new Exception("Usage Location not found.");
		if(!contractorRepo.existsById(oiData.getContractorId()))
			throw new Exception("Contractor not found.");
		if(!warehouseRepo.existsById(oiData.getWarehouseId()))
			throw new Exception("Contractor not found.");
		
		if(!usageAreaRepo.existsById(oiData.getUsageAreaId()))
			throw new Exception("Usage Area not found.");
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
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		updateStockBeforeDelete(outwardInventory);
		outwardInventoryRepo.softDeleteById(id);
	}

	private void updateStockBeforeDelete(OutwardInventory outwardInventory) throws Exception 
	{
		String warehouseName = outwardInventory.getWarehouse().getWarehouseName();
		for(InwardOutwardList ioList : outwardInventory.getInwardOutwardList())
		{
			Double stock = ioList.getQuantity();
			Double currentStock = stockRepo.findStockForProductAndWarehouse(ioList.getProduct().getProductId(),warehouseName).get(0).getQuantityInHand();
			if(currentStock<stock)
				throw new Exception("Cannot Delete. Stock will go negative if deleted");
			stockService.updateStock(ioList.getProduct().getProductId(), warehouseName, stock, "inward");
		}
	}
}
