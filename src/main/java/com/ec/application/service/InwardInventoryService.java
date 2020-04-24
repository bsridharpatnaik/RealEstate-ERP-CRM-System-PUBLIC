package com.ec.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnInwardInventoryData;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.InwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.application.repository.WarehouseRepo;

@Service
public class InwardInventoryService 
{

	@Autowired
	InwardInventoryRepo inwardInventoryRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	ProductService productService;
	
	
	@Autowired
	SupplierRepo supplierRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	@Autowired
	StockService stockService;
	
	@Autowired
	StockRepo stockRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;
	
	@Transactional
	public InwardInventory createInwardnventory(InwardInventoryData iiData) throws Exception
	{
		InwardInventory inwardInventory = new InwardInventory();
		validateInputs(iiData);
		setFields(inwardInventory,iiData);
		updateStockForCreateInwardInventory(inwardInventory);
		return inwardInventoryRepo.save(inwardInventory);
	}
	
	private void updateStockForCreateInwardInventory(InwardInventory inwardInventory) throws Exception 
	{
		Set<InwardOutwardList> productsWithQuantities = inwardInventory.getInwardOutwardList();
		String warehouseName = inwardInventory.getWarehouse().getWarehouseName();
		
		for(InwardOutwardList oiList : productsWithQuantities)
		{
			Long productId = oiList.getProduct().getProductId();
			Float quantity =  oiList.getQuantity();
			Float closingStock = stockService.updateStock(productId, warehouseName, quantity, "inward");
			oiList.setClosingStock(closingStock);
		}
	}

	private void setFields(InwardInventory inwardInventory, InwardInventoryData iiData) throws Exception 
	{
		inwardInventory.setDate(iiData.getDate());
		inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
		inwardInventory.setVehicleNo(iiData.getVehicleNo());
		inwardInventory.setVendorSlipNo(iiData.getVendorSlipNo());
		inwardInventory.setAdditionalInfo(iiData.getAdditionalComments());
		inwardInventory.setSupplier(supplierRepo.findById(iiData.getSupplierId()).get());
		inwardInventory.setWarehouse(warehouseRepo.findById(iiData.getWarehouseId()).get());
		inwardInventory.setInwardOutwardList(fetchInwardOutwardList(iiData.getProductWithQuantities(),warehouseRepo.findById(iiData.getWarehouseId()).get()));
	}

	private Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities, Warehouse warehouse) 
	{
		Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
		for(ProductWithQuantity productWithQuantity : productWithQuantities)
		{
			InwardOutwardList inwardOutwardList = new InwardOutwardList();
			Product product = productRepo.findById(productWithQuantity.getProductId()).get();
			inwardOutwardList.setProduct(product);
			inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
			inwardOutwardListSet.add(inwardOutwardList);
		}
		return inwardOutwardListSet;
	}

	private void validateInputs(InwardInventoryData iiData) throws Exception 
	{
		for(ProductWithQuantity productWithQuantity : iiData.getProductWithQuantities())
		{
			if(!productRepo.existsById(productWithQuantity.getProductId()))
				throw new Exception("Product not found with ID "+ productWithQuantity.getProductId());
			if(productWithQuantity.getQuantity()<=0)
				throw new Exception("Quantity cannot be less that or equals zero");
		}
		if(!supplierRepo.existsById(iiData.getSupplierId()))
			throw new Exception("Supplier not found with ID");
		if(!warehouseRepo.existsById(iiData.getWarehouseId()))
			throw new Exception("Warehouse not found");
	}

	public ReturnInwardInventoryData fetchInwardnventory(Pageable pageable) 
	{
		ReturnInwardInventoryData returnInwardInventoryData = new ReturnInwardInventoryData();
		returnInwardInventoryData.setInwardInventory(inwardInventoryRepo.findAll(pageable));
		returnInwardInventoryData.setIiDropdown(populateDropdownService.fetchData("inward"));
		return returnInwardInventoryData;
	}
}
/*
	

}

	public InwardInventory findInwardnventory(Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(inwardInventoryOpt.isPresent()==false)
			throw new Exception("Inward inventory with ID not found");
		return inwardInventoryOpt.get();
		
	}
	public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(!inwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		Long oldProductId = inwardInventory.getProduct().getProductId();
		Float oldQuantity = stockRepo.findStockForProductAsList(inwardInventory.getProduct().getProductId()).get(0).getQuantityInHand();
		validateInputs(iiData);
		setFields(inwardInventory,iiData);
		updateStock(oldProductId,iiData.getProductId(),inwardInventory,iiData.getQuantity(),oldQuantity);
		return inwardInventoryRepo.save(inwardInventory);
		
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

	public InwardInventoryWithDropdownValues findAll(Pageable pageable) 
	{
		InwardInventoryWithDropdownValues inwardInventoryWithDropdownValues = new InwardInventoryWithDropdownValues();
		inwardInventoryWithDropdownValues.setMorDropdown(populateDropdownService.fetchData("inward"));
		inwardInventoryWithDropdownValues.setInwardInventory(inwardInventoryRepo.findAll(pageable));
		return inwardInventoryWithDropdownValues;
	}
	
	public void deleteInwardnventory(Long id) throws Exception 
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(!inwardInventoryOpt.isPresent())
			throw new Exception("Inward Inventory with ID not found");
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		updateStockBeforeDelete(inwardInventory);
		inwardInventoryRepo.softDeleteById(id);
	}
	private void updateStockBeforeDelete(InwardInventory inwardInventory) throws Exception 
	{
		Float stock = inwardInventory.getQuantity();
		Float currentStock = stockRepo.findStockForProductAsList(inwardInventory.getProduct().getProductId()).get(0).getQuantityInHand();
		if(currentStock<stock)
			throw new Exception("Cannot Delete. Stock will go negative if deleted");
		stockService.updateStock(inwardInventory.getProduct().getProductId(), "Default", stock, "outward");
	}
} */
