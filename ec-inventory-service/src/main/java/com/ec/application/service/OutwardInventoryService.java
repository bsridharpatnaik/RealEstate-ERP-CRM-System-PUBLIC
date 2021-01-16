package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.OutwardInventoryExportDAO;
import com.ec.application.data.OutwardInventoryExportDAO2;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnOutwardData;
import com.ec.application.data.ReturnOutwardInventoryData;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.OutwardInventory_;
import com.ec.application.model.ReturnOutwardList;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.UsageAreaRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.OutwardInventorySpecification;

@Service
@Transactional
public class OutwardInventoryService
{
	@Autowired
	ProductService productService;

	@Autowired
	InwardOutwardListRepo iolRepo;

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

	@Autowired
	GroupBySpecification groupBySpecification;

	Logger log = LoggerFactory.getLogger(OutwardInventoryService.class);

	@Transactional(rollbackFor = Exception.class)
	public OutwardInventory createOutwardnventory(OutwardInventoryData oiData) throws Exception
	{
		log.info("Invoked createOutwardnventory with payload -" + oiData.toString());
		OutwardInventory outwardInventory = new OutwardInventory();
		validateInputs(oiData);
		log.info("Payload validated");
		log.info("Setting fields");
		setFields(outwardInventory, oiData);
		log.info("Updating stock");
		updateStockForCreateOutwardInventory(outwardInventory);
		log.info("Saving dat to outward repository");
		return outwardInventoryRepo.save(outwardInventory);

	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockForCreateOutwardInventory(OutwardInventory outwardInventory) throws Exception
	{
		log.info("Invoked updateStockForCreateOutwardInventory");
		Set<InwardOutwardList> productsWithQuantities = outwardInventory.getInwardOutwardList();
		String warehouseName = outwardInventory.getWarehouse().getWarehouseName();

		for (InwardOutwardList oiList : productsWithQuantities)
		{
			Long productId = oiList.getProduct().getProductId();
			Double quantity = oiList.getQuantity();
			Double closingStock = stockService.updateStock(productId, warehouseName, quantity, "outward");
			oiList.setClosingStock(closingStock);
		}
		log.info("Exited updateStockForCreateOutwardInventory");
	}

	@Transactional(rollbackFor = Exception.class)
	public OutwardInventory addReturnEntry(ReturnOutwardData rd, Long outwardId) throws Exception
	{
		log.info("Invoked return inventory for outward id -" + outwardId + " With data " + rd.toString());
		if (!outwardInventoryRepo.existsById(outwardId))
			throw new Exception("Outward inventory with ID not found");

		Long duplicateProductIdCount = rd.getProductWithQuantities().stream()
				.collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
				.filter(e -> e.getValue() > 1).count();

		if (duplicateProductIdCount > 0)
			throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");

		for (ProductWithQuantity productWithQuantity : rd.getProductWithQuantities())
		{
			log.info("Processing return for product " + productWithQuantity.getProductId());
			if (productWithQuantity.getQuantity() == null || productWithQuantity.getProductId() == null
					|| !productRepo.existsById(productWithQuantity.getProductId()))
				throw new Exception("Error fetching product details");
			addReturnForOutward(outwardId, productWithQuantity.getProductId(), productWithQuantity.getQuantity());
		}
		return outwardInventoryRepo.getOne(outwardId);
	}

	@Transactional(rollbackFor = Exception.class)
	private void addReturnForOutward(Long outwardId, Long productId, Double quantity) throws Exception
	{

		OutwardInventory oi = outwardInventoryRepo.getOne(outwardId);
		Set<ReturnOutwardList> returnOutwardList = oi.getReturnOutwardList();
		Set<InwardOutwardList> inwardOutwardListSet = oi.getInwardOutwardList();
		for (InwardOutwardList inwardOutwardList : inwardOutwardListSet)
		{
			if (inwardOutwardList.getProduct().getProductId().equals(productId))
			{
				log.info("Product found in existing list -" + productId);
				Double currentQuantity = inwardOutwardList.getQuantity();
				if (quantity >= currentQuantity)
					throw new Exception(
							"Return quantity cannot be greater than or equals to existing quantity for product -"
									+ inwardOutwardList.getProduct().getProductName());

				Double diffInQuantity = currentQuantity - quantity;
				log.info("Difference in quantity -" + diffInQuantity);
				Double closingStock = stockService.updateStock(productId, oi.getWarehouse().getWarehouseName(),
						quantity, "inward");
				log.info("Updated stock -" + closingStock);
				returnOutwardList.add(new ReturnOutwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
						quantity, closingStock));
				inwardOutwardList.setQuantity(diffInQuantity);
				inwardOutwardList.setClosingStock(closingStock);
			}
		}
		oi.setReturnOutwardList(returnOutwardList);
		oi.setInwardOutwardList(inwardOutwardListSet);
		outwardInventoryRepo.save(oi);
	}

	@Transactional(rollbackFor = Exception.class)
	public OutwardInventory updateOutwardnventory(OutwardInventoryData iiData, Long id) throws Exception
	{
		log.info("Invoked updateOutwardnventory");
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if (!outwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		validateInputs(iiData);
		exitIfReturnExists(outwardInventoryOpt.get(), iiData);
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		OutwardInventory oldOutwardInventory = (OutwardInventory) outwardInventory.clone();
		setFields(outwardInventory, iiData);
		modifyStockBeforeUpdate(oldOutwardInventory, outwardInventory);
		log.info("Exiting updateOutwardnventory");
		removeOrphans(oldOutwardInventory);
		return outwardInventoryRepo.save(outwardInventory);

	}

	private void exitIfReturnExists(OutwardInventory outwardInventory, OutwardInventoryData iiData) throws Exception
	{
		if (outwardInventory.getReturnOutwardList().size() > 0)
			throw new Exception("Outward inventory entry cannot be edited after return entry is added.");
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateWhenWarehouseSame(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
			throws Exception
	{
		log.info("Invoked updateWhenWarehouseSame");
		// Fetch product only in old and only in new and common
		Set<Long> oldProductSet = new HashSet<>(oldOutwardInventory.getInwardOutwardList().size());
		Set<Long> newProductSet = new HashSet<>(outwardInventory.getInwardOutwardList().size());
		oldOutwardInventory.getInwardOutwardList().stream()
				.filter(p -> oldProductSet.add(p.getProduct().getProductId())).collect(Collectors.toList());
		outwardInventory.getInwardOutwardList().stream().filter(p -> newProductSet.add(p.getProduct().getProductId()))
				.collect(Collectors.toList());
		Set<Long> onlyInOld = ReusableMethods.differenceBetweenSets(oldProductSet, newProductSet);
		Set<Long> onlyInNew = ReusableMethods.differenceBetweenSets(newProductSet, oldProductSet);
		Set<Long> commonInBoth = ReusableMethods.commonBetweenSets(oldProductSet, newProductSet);
		updateStockForOnlyInOld(onlyInOld, oldOutwardInventory);
		updateStockForOnlyInNew(onlyInNew, outwardInventory);
		updateStockForCommonInBoth(commonInBoth, oldOutwardInventory, outwardInventory);
		log.info("Exiting updateWhenWarehouseSame");
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockForCommonInBoth(Set<Long> commonInBoth, OutwardInventory oldOutwardInventory,
			OutwardInventory outwardInventory) throws Exception
	{
		log.info("Invoked updateWhenWarehouseSame");
		Set<InwardOutwardList> oldIOListSet = oldOutwardInventory.getInwardOutwardList();
		Set<InwardOutwardList> newIOListSet = outwardInventory.getInwardOutwardList();
		for (Long id : commonInBoth)
		{
			Double oldQuantity = findQuantityForProductInIOList(id, oldIOListSet);
			Double newQuantity = findQuantityForProductInIOList(id, newIOListSet);
			Double quantityForUpdate = newQuantity - oldQuantity;
			for (InwardOutwardList ioList : newIOListSet)
			{
				if (id.equals(ioList.getProduct().getProductId()))
				{
					Double closingStock = stockService.updateStock(id,
							outwardInventory.getWarehouse().getWarehouseName(), quantityForUpdate, "outward");
					System.out.println("Closing stock - " + closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
					ioList.setClosingStock(closingStock);
				}
			}
			outwardInventory.setInwardOutwardList(newIOListSet);
		}
		log.info("Exiting updateWhenWarehouseSame");
	}

	@Transactional(rollbackFor = Exception.class)
	private Double findQuantityForProductInIOList(Long productId, Set<InwardOutwardList> ioListSet)
	{
		log.info("Invoked findQuantityForProductInIOList");
		for (InwardOutwardList ioList : ioListSet)
		{
			if (productId.equals(ioList.getProduct().getProductId()))
			{
				log.info("Old Quantity in findQuantityForProductInIOList - " + ioList.getQuantity());
				Double oldQuantity = ioList.getQuantity();
				log.info("Exiting findQuantityForProductInIOList");
				return oldQuantity;
			}
		}
		log.info("Exiting findQuantityForProductInIOList with return as null");
		return null;
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockForOnlyInNew(Set<Long> onlyInNew, OutwardInventory outwardInventory) throws Exception
	{
		log.info("Invoked updateStockForOnlyInNew");
		for (Long id : onlyInNew)
		{
			Set<InwardOutwardList> ioListSet = outwardInventory.getInwardOutwardList();
			for (InwardOutwardList ioList : ioListSet)
			{
				if (id.equals(ioList.getProduct().getProductId()))
				{
					log.info("Element in old list but not in new - " + id);
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id,
							outwardInventory.getWarehouse().getWarehouseName(), quantity, "outward");
					log.info("Closing stock - " + closingStock);
					log.info("Pushing inventory notification");
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							outwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
					log.info("Setting closing stock");
					ioList.setClosingStock(closingStock);
				}
			}
			outwardInventory.setInwardOutwardList(ioListSet);
			log.info("Exiting updateStockForOnlyInNew");
		}
		log.info("Exiting updateStockForOnlyInNew");
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockForOnlyInOld(Set<Long> onlyInOld, OutwardInventory oldOutwardInventory) throws Exception
	{
		log.info("Invoked updateStockForOnlyInOld");
		// Delete stock received as part of old inventory
		for (Long id : onlyInOld)
		{
			Set<InwardOutwardList> ioListSet = oldOutwardInventory.getInwardOutwardList();
			for (InwardOutwardList ioList : ioListSet)
			{
				if (id.equals(ioList.getProduct().getProductId()))
				{
					log.info("Element in old list but not in new - " + id);
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id,
							oldOutwardInventory.getWarehouse().getWarehouseName(), quantity, "inward");
					log.info("Closing stock - " + closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							oldOutwardInventory.getWarehouse().getWarehouseName(), "outward", closingStock);
				}
			}
		}
		log.info("Exiting updateStockForOnlyInOld");
	}

	@Transactional(rollbackFor = Exception.class)
	private void modifyStockBeforeUpdate(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
			throws Exception
	{
		log.info("Invoked modifyStockBeforeUpdate");
		if (!oldOutwardInventory.getWarehouse().getWarehouseId()
				.equals(outwardInventory.getWarehouse().getWarehouseId()))
			updateWhenWarehouseChanged(oldOutwardInventory, outwardInventory);
		else
			updateWhenWarehouseSame(oldOutwardInventory, outwardInventory);
		log.info("Exiting modifyStockBeforeUpdate");
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateWhenWarehouseChanged(OutwardInventory oldOutwardInventory, OutwardInventory outwardInventory)
			throws Exception
	{
		log.info("Invoked updateWhenWarehouseChanged");
		// Delete all stock added as part of old warehouse
		traverseListAndUpdateStock(oldOutwardInventory.getInwardOutwardList(), "inward",
				oldOutwardInventory.getWarehouse());

		// Add new stock to new warehouse
		Set<InwardOutwardList> newLIOList = traverseListAndUpdateStock(outwardInventory.getInwardOutwardList(),
				"outward", outwardInventory.getWarehouse());
		outwardInventory.setInwardOutwardList(newLIOList);
		log.info("Existing updateWhenWarehouseChanged");
	}

	@Transactional(rollbackFor = Exception.class)
	private Set<InwardOutwardList> traverseListAndUpdateStock(Set<InwardOutwardList> ioListset, String type,
			Warehouse warehouse) throws Exception
	{
		log.info("Invoked traverseListAndUpdateStock");
		for (InwardOutwardList oiList : ioListset)
		{
			Double closingStock = stockService.updateStock(oiList.getProduct().getProductId(),
					warehouse.getWarehouseName(), oiList.getQuantity(), type);
			log.info("Pushing quantity edited notification");
			inventoryNotificationService.pushQuantityEditedNotification(oiList.getProduct(),
					warehouse.getWarehouseName(), "outward", closingStock);
			oiList.setClosingStock(closingStock);
			log.info("Exiting traverseListAndUpdateStock");
		}
		log.info("Exiting traverseListAndUpdateStock");
		return ioListset;
	}

	private void setFields(OutwardInventory outwardInventory, OutwardInventoryData oiData)
	{
		log.info("Invoked setFields");
		Warehouse warehouse = warehouseRepo.findById(oiData.getWarehouseId()).get();
		outwardInventory.setAdditionalInfo(oiData.getAdditionalInfo());
		outwardInventory.setContractor(contractorRepo.findById(oiData.getContractorId()).get());
		outwardInventory.setUsageLocation(locationRepo.findById(oiData.getUsageLocationId()).get());
		outwardInventory.setUsageArea(usageAreaRepo.findById(oiData.getUsageAreaId()).get());
		outwardInventory.setWarehouse(warehouse);
		;
		outwardInventory.setDate(oiData.getDate());
		outwardInventory.setPurpose(oiData.getPurpose());
		outwardInventory.setSlipNo(oiData.getSlipNo());
		outwardInventory
				.setInwardOutwardList(iiService.fetchInwardOutwardList(oiData.getProductWithQuantities(), warehouse));
		outwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(oiData.getFileInformations()));
		log.info("Exited setFields");
	}

	private void validateInputs(OutwardInventoryData oiData) throws Exception
	{
		log.info("Invoked validateInputs");
		if (!locationRepo.existsById(oiData.getUsageLocationId()))
			throw new Exception("Usage Location not found.");
		if (!contractorRepo.existsById(oiData.getContractorId()))
			throw new Exception("Contractor not found.");
		if (!warehouseRepo.existsById(oiData.getWarehouseId()))
			throw new Exception("Contractor not found.");
		if (!usageAreaRepo.existsById(oiData.getUsageAreaId()))
			throw new Exception("Usage Area not found.");
		Long duplicateProductIdCount = oiData.getProductWithQuantities().stream()
				.collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
				.filter(e -> e.getValue() > 1).count();

		if (duplicateProductIdCount > 0)
			throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");
		for (ProductWithQuantity productWithQuantity : oiData.getProductWithQuantities())
		{
			if (!productRepo.existsById(productWithQuantity.getProductId()))
				throw new Exception("Product not found.");
			if (productWithQuantity.getQuantity() <= 0)
				throw new Exception("Quantity should be greater than zero");
		}
		log.info("Exited validateInputs");
	}

	public OutwardInventory findOutwardnventory(Long id) throws Exception
	{
		log.info("Invoked findOutwardnventory");
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if (outwardInventoryOpt.isPresent() == false)
			throw new Exception("Outward inventory with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		log.info("Exited findOutwardnventory");
		return outwardInventory;
	}

	public ReturnOutwardInventoryData fetchOutwardnventory(FilterDataList filterDataList, Pageable pageable)
			throws ParseException
	{
		log.info("Invoked fetchOutwardnventory");
		ReturnOutwardInventoryData returnOutwardInventoryData = new ReturnOutwardInventoryData();

		// Feed data list
		Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
		if (spec != null)
			returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(spec, pageable));
		else
			returnOutwardInventoryData.setOutwardInventory(outwardInventoryRepo.findAll(pageable));

		// Feed dropdown values
		returnOutwardInventoryData.setIiDropdown(populateDropdownService.fetchData("outward"));

		// Feed totals
		returnOutwardInventoryData.setTotals(spec != null ? fetchGroupingForFilteredData(spec, OutwardInventory.class)
				: fetchOutwardnventoryGroupBy());
		log.info("Exited fetchOutwardnventory");
		return returnOutwardInventoryData;
	}

	public List<ProductGroupedDAO> fetchOutwardnventoryGroupBy() throws ParseException
	{
		log.info("Invoked fetchOutwardnventoryGroupBy");
		List<ProductGroupedDAO> groupedData = outwardInventoryRepo.findGroupByInfo();
		log.info("Exiing fetchOutwardnventoryGroupBy");
		return groupedData;
	}

	private List<ProductGroupedDAO> fetchGroupingForFilteredData(Specification<OutwardInventory> spec,
			Class<OutwardInventory> class1)
	{
		log.info("Invoked fetchGroupingForFilteredData");
		List<ProductGroupedDAO> groupedData = groupBySpecification.findDataByConfiguration(spec, OutwardInventory.class,
				OutwardInventory_.INWARD_OUTWARD_LIST);
		log.info("Exiting fetchGroupingForFilteredData");
		return groupedData;
	}

	public List<OutwardInventoryExportDAO> fetchOutwardnventoryForExport(FilterDataList filterDataList) throws Exception
	{
		log.info("Invoked fetchOutwardnventoryForExport");
		Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
		long size = spec != null ? outwardInventoryRepo.count(spec) : outwardInventoryRepo.count();
		if (size > 5000)
			throw new Exception("Too many rows to export. Apply some more filters and try again");
		List<OutwardInventory> iiData = spec != null ? outwardInventoryRepo.findAll(spec)
				: outwardInventoryRepo.findAll();
		List<OutwardInventoryExportDAO> clonedData = iiData.parallelStream().map(OutwardInventoryExportDAO::new)
				.collect(Collectors.toList());
		log.info("Exited fetchOutwardnventoryForExport");
		return clonedData;
	}

	public List<OutwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList)
			throws Exception
	{
		log.info("Invoked fetchInwardnventoryForExport2");
		Specification<OutwardInventory> spec = OutwardInventorySpecification.getSpecification(filterDataList);
		long size = spec != null ? outwardInventoryRepo.count(spec) : outwardInventoryRepo.count();
		System.out.println("Size of inward inventory after filter -" + size);
		if (size > 2000)
			throw new Exception("Too many rows to export. Apply some more filters and try again");
		System.out.println("Fetching data from db");
		List<OutwardInventory> iiData = spec != null ? outwardInventoryRepo.findAll(spec)
				: outwardInventoryRepo.findAll();
		List<OutwardInventoryExportDAO2> clonedData = transformDataForExport(iiData);
		System.out.println("Completed - returning to controller");
		log.info("Exited fetchInwardnventoryForExport2");
		return clonedData;
	}

	private List<OutwardInventoryExportDAO2> transformDataForExport(List<OutwardInventory> iiData)
	{
		log.info("Invoked transformDataForExport");
		List<OutwardInventoryExportDAO2> transformedData = new ArrayList<OutwardInventoryExportDAO2>();
		for (OutwardInventory ii : iiData)
		{
			for (InwardOutwardList ioList : ii.getInwardOutwardList())
			{
				OutwardInventoryExportDAO2 ied = new OutwardInventoryExportDAO2(ii, ioList);
				transformedData.add(ied);
			}
		}
		log.info("Exiting transformDataForExport");
		return transformedData;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteOutwardInventoryById(Long id) throws Exception
	{
		log.info("Invoked deleteOutwardInventoryById");
		Optional<OutwardInventory> outwardInventoryOpt = outwardInventoryRepo.findById(id);
		if (!outwardInventoryOpt.isPresent())
			throw new Exception("Outward Inventory with ID not found");
		OutwardInventory outwardInventory = outwardInventoryOpt.get();
		updateStockBeforeDelete(outwardInventory);
		removeOrphans(outwardInventory);
		outwardInventoryRepo.softDeleteById(id);
		log.info("Exiting deleteOutwardInventoryById");
	}

	@Transactional(rollbackFor = Exception.class)
	private void removeOrphans(OutwardInventory outwardInventory)
	{
		Set<InwardOutwardList> iolList = outwardInventory.getInwardOutwardList();
		for (InwardOutwardList iol : iolList)
		{
			iol.setDeleted(true);
			iolRepo.save(iol);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockBeforeDelete(OutwardInventory outwardInventory) throws Exception
	{
		log.info("Invoked updateStockBeforeDelete");
		String warehouseName = outwardInventory.getWarehouse().getWarehouseName();
		for (InwardOutwardList ioList : outwardInventory.getInwardOutwardList())
		{
			Double stock = ioList.getQuantity();
			Double currentStock = stockRepo
					.findStockForProductAndWarehouse(ioList.getProduct().getProductId(), warehouseName).get(0)
					.getQuantityInHand();
			if (currentStock < stock)
				throw new Exception("Cannot Delete. Stock will go negative if deleted");
			stockService.updateStock(ioList.getProduct().getProductId(), warehouseName, stock, "inward");
		}
		log.info("Exiting updateStockBeforeDelete");
	}
}
