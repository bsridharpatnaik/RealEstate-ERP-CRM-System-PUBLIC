package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ReusableMethods;
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
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.InwardInventorySpecification;

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
	
	@Transactional
	private void updateStockForCreateInwardInventory(InwardInventory inwardInventory) throws Exception 
	{
		Set<InwardOutwardList> productsWithQuantities = inwardInventory.getInwardOutwardList();
		String warehouseName = inwardInventory.getWarehouse().getWarehouseName();
		
		for(InwardOutwardList oiList : productsWithQuantities)
		{
			Long productId = oiList.getProduct().getProductId();
			Double quantity =  oiList.getQuantity();
			Double closingStock = stockService.updateStock(productId, warehouseName, quantity, "inward");
			oiList.setClosingStock(closingStock);
		}
	}

	private void setFields(InwardInventory inwardInventory, InwardInventoryData iiData) throws Exception 
	{
		inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
		inwardInventory.setDate(iiData.getDate());
		inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
		inwardInventory.setVehicleNo(iiData.getVehicleNo());
		inwardInventory.setVendorSlipNo(iiData.getVendorSlipNo());
		inwardInventory.setAdditionalInfo(iiData.getAdditionalComments());
		inwardInventory.setSupplier(supplierRepo.findById(iiData.getSupplierId()).get());
		inwardInventory.setWarehouse(warehouseRepo.findById(iiData.getWarehouseId()).get());
		inwardInventory.setInwardOutwardList(fetchInwardOutwardList(iiData.getProductWithQuantities(),warehouseRepo.findById(iiData.getWarehouseId()).get()));
		inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
	}

	public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities, Warehouse warehouse) 
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
		
		 Long duplicateProductIdCount = iiData.getProductWithQuantities().stream()
        	.collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream().filter(e -> e.getValue() > 1).count();
		
		if(duplicateProductIdCount>0)
			throw new Exception("Duplicate product IDs found in request");
	}

	public ReturnInwardInventoryData fetchInwardnventory(FilterDataList filterDataList,Pageable pageable) throws ParseException 
	{
		ReturnInwardInventoryData returnInwardInventoryData = new ReturnInwardInventoryData();
		
		Specification<InwardInventory> spec = InwardInventorySpecification.getSpecification(filterDataList);
		
		if(spec!=null) returnInwardInventoryData.setInwardInventory(inwardInventoryRepo.findAll(spec,pageable));
		else returnInwardInventoryData.setInwardInventory(inwardInventoryRepo.findAll(pageable));
		returnInwardInventoryData.setIiDropdown(populateDropdownService.fetchData("inward"));
		return returnInwardInventoryData;
	}

	public InwardInventory findById(long id) throws Exception 
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(inwardInventoryOpt.isPresent())
			return inwardInventoryOpt.get();
		else 
			throw new Exception("Inward inventory not found");
	}
	
	@Transactional
	public void deleteInwardInventoryById(Long id) throws Exception 
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(!inwardInventoryOpt.isPresent())
			throw new Exception("Inward Inventory with ID not found");
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		updateStockBeforeDelete(inwardInventory);
		inwardInventoryRepo.softDeleteById(id);
		
	}
	@Transactional
	private void updateStockBeforeDelete(InwardInventory inwardInventory) throws Exception 
	{
		String warehouseName = inwardInventory.getWarehouse().getWarehouseName();
		for(InwardOutwardList ioList : inwardInventory.getInwardOutwardList())
		{
			Double stock = ioList.getQuantity();
			Double currentStock = stockRepo.findStockForProductAndWarehouse(ioList.getProduct().getProductId(),warehouseName).get(0).getQuantityInHand();
			if(currentStock<stock)
				throw new Exception("Cannot Delete. Stock will go negative if deleted");
			stockService.updateStock(ioList.getProduct().getProductId(), warehouseName, stock, "outward");
		}
	}
	
	@Transactional
	public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if(!inwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		validateInputs(iiData);
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		updateStockBeforeDelete(inwardInventory);
		setFields(inwardInventory,iiData);
		updateStockForCreateInwardInventory(inwardInventory);
		return inwardInventoryRepo.save(inwardInventory);
		
	}
}
