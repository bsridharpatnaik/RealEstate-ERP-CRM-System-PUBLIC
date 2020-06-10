package com.ec.application.service;

import java.text.ParseException;
import java.util.ArrayList;
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
import com.ec.application.data.DashboardInwardOutwardInventoryDAO;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnOutwardInventoryData;
import com.ec.application.model.InwardInventory;
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
	
	@Autowired
	InventoryNotificationService inventoryNotificationService;
	
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
		validateInputs(iiData);
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		OutwardInventory oldOutwardInventory = (OutwardInventory) outwardInventory.clone();
		setFields(outwardInventory,iiData);
		modifyStockBeforeUpdate(oldOutwardInventory,outwardInventory);
		return outwardInventoryRepo.save(outwardInventory);
		
	}
	
	@Transactional
	private void updateWhenWarehouseSame(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory) throws Exception 
	{
		//Fetch product only in old and only in new and common
		Set<Long> oldProductSet = new HashSet<>(oldOutwardInventory.getInwardOutwardList().size());
		Set<Long> newProductSet = new HashSet<>(outwardInventory.getInwardOutwardList().size());
		oldOutwardInventory.getInwardOutwardList().stream().filter(p -> oldProductSet.add(p.getProduct().getProductId())).collect(Collectors.toList());
		outwardInventory.getInwardOutwardList().stream().filter(p -> newProductSet.add(p.getProduct().getProductId())).collect(Collectors.toList());
		Set<Long> onlyInOld = ReusableMethods.differenceBetweenSets(oldProductSet,newProductSet);
		Set<Long> onlyInNew = ReusableMethods.differenceBetweenSets(newProductSet,oldProductSet);
		Set<Long> commonInBoth = ReusableMethods.commonBetweenSets(oldProductSet, newProductSet);
		updateStockForOnlyInOld(onlyInOld,oldOutwardInventory);
		updateStockForOnlyInNew(onlyInNew,outwardInventory);
		updateStockForCommonInBoth(commonInBoth,oldOutwardInventory,outwardInventory);
	}
	@Transactional
	private void updateStockForCommonInBoth(Set<Long> commonInBoth, OutwardInventory oldOutwardInventory,
			OutwardInventory outwardInventory) throws Exception 
	{
		Set<InwardOutwardList> oldIOListSet = oldOutwardInventory.getInwardOutwardList();
		Set<InwardOutwardList> newIOListSet = outwardInventory.getInwardOutwardList();
		for(Long id:commonInBoth)
		{
			Double oldQuantity = findQuantityForProductInIOList(id,oldIOListSet);
			Double newQuantity = findQuantityForProductInIOList(id,newIOListSet);
			Double quantityForUpdate = newQuantity - oldQuantity;
			for(InwardOutwardList ioList:newIOListSet)
			{
				if(id.equals(ioList.getProduct().getProductId()) && quantityForUpdate!=0)
				{
					Double closingStock = stockService.updateStock(id, outwardInventory.getWarehouse().getWarehouseName(), quantityForUpdate, "outward");
					System.out.println("Closing stock - "+closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
					ioList.setClosingStock(closingStock);
				}
			}
			outwardInventory.setInwardOutwardList(newIOListSet);
		}
		
	}
	
	@Transactional
	private Double findQuantityForProductInIOList(Long productId,Set<InwardOutwardList> ioListSet) 
	{
		for(InwardOutwardList ioList:ioListSet)
		{
			if(productId.equals(ioList.getProduct().getProductId()))
			{
				Double oldQuantity = ioList.getQuantity();
				return oldQuantity;
			}
		}
		return null;
	}
	@Transactional
	private void updateStockForOnlyInNew(Set<Long> onlyInNew, OutwardInventory outwardInventory) throws Exception 
	{
		for(Long id:onlyInNew)
		{
			Set<InwardOutwardList> ioListSet = outwardInventory.getInwardOutwardList();
			for(InwardOutwardList ioList:ioListSet)
			{
				if(id.equals(ioList.getProduct().getProductId()))
				{
					System.out.println("Element in old list but not in new - " + id );
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id, outwardInventory.getWarehouse().getWarehouseName(), quantity, "outward");
					System.out.println("Closing stock - "+closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
					ioList.setClosingStock(closingStock);
				}
			}
			outwardInventory.setInwardOutwardList(ioListSet);
		}
		
	}
	@Transactional
	private void updateStockForOnlyInOld(Set<Long> onlyInOld, OutwardInventory oldOutwardInventory) throws Exception 
	{
		//Delete stock received as part of old inventory
		for(Long id:onlyInOld)
		{
			Set<InwardOutwardList> ioListSet = oldOutwardInventory.getInwardOutwardList();
			for(InwardOutwardList ioList:ioListSet)
			{
				if(id.equals(ioList.getProduct().getProductId()))
				{
					System.out.println("Element in old list but not in new - " + id );
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id, oldOutwardInventory.getWarehouse().getWarehouseName(), quantity, "inward");
					System.out.println("Closing stock - "+closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),oldOutwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
				}
			}
		}
	}
	@Transactional
	private void modifyStockBeforeUpdate(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory) throws Exception 
	{
		if(!oldOutwardInventory.getWarehouse().getWarehouseId().equals(outwardInventory.getWarehouse().getWarehouseId()))
			updateWhenWarehouseChanged(oldOutwardInventory,outwardInventory);
		else
			updateWhenWarehouseSame(oldOutwardInventory,outwardInventory);
	}
	
	@Transactional
	private void updateWhenWarehouseChanged(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory) throws Exception 
	{
		//Delete all stock added as part of old warehouse
		traverseListAndUpdateStock(oldOutwardInventory.getInwardOutwardList(),"inward",oldOutwardInventory.getWarehouse());
		
		//Add new stock to new warehouse
		Set<InwardOutwardList> newLIOList = traverseListAndUpdateStock(outwardInventory.getInwardOutwardList(),"outward",outwardInventory.getWarehouse());
		outwardInventory.setInwardOutwardList(newLIOList);
	}
	
	@Transactional
	private Set<InwardOutwardList> traverseListAndUpdateStock(Set<InwardOutwardList> ioListset,String type,Warehouse warehouse) throws Exception
	{
		for(InwardOutwardList oiList : ioListset)
		{
			Double closingStock = stockService.updateStock(oiList.getProduct().getProductId(), warehouse.getWarehouseName(),oiList.getQuantity() , type);
			inventoryNotificationService.pushQuantityEditedNotification(oiList.getProduct(),warehouse.getWarehouseName(), "outward", closingStock);
			oiList.setClosingStock(closingStock);
		}
		return ioListset;
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
