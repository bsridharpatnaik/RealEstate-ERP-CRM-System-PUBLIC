package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.ArrayList;
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
import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.InwardInventoryExportDAO2;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnInwardInventoryData;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.Product;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.InwardInventoryRepo;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.InwardInventorySpecification;

@Service
@Transactional
public class InwardInventoryService
{

	Logger logger = LoggerFactory.getLogger(AllInventoryService.class);

	@Autowired
	InwardInventoryRepo inwardInventoryRepo;

	@Autowired
	InwardOutwardListRepo iolRepo;

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

	@Autowired
	GroupBySpecification groupBySpecification;

	@Autowired
	InventoryNotificationService inventoryNotificationService;

	@Transactional
	public InwardInventory createInwardnventory(InwardInventoryData iiData) throws Exception
	{
		InwardInventory inwardInventory = new InwardInventory();
		validateInputs(iiData);
		setFields(inwardInventory, iiData);
		updateStockForCreateInwardInventory(inwardInventory);
		return inwardInventoryRepo.save(inwardInventory);
	}

	@Transactional
	private void updateStockForCreateInwardInventory(InwardInventory inwardInventory) throws Exception
	{
		Set<InwardOutwardList> productsWithQuantities = inwardInventory.getInwardOutwardList();
		String warehouseName = inwardInventory.getWarehouse().getWarehouseName();

		for (InwardOutwardList oiList : productsWithQuantities)
		{
			Long productId = oiList.getProduct().getProductId();
			Double quantity = oiList.getQuantity();
			Double closingStock = stockService.updateStock(productId, warehouseName, quantity, "inward");
			oiList.setClosingStock(closingStock);
		}
	}

	private void setFields(InwardInventory inwardInventory, InwardInventoryData iiData) throws Exception
	{
		inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
		// inwardInventory.setInvoiceReceived(false);
		inwardInventory.setDate(iiData.getDate());
		inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
		inwardInventory.setVehicleNo(iiData.getVehicleNo());
		inwardInventory.setVendorSlipNo(iiData.getVendorSlipNo());
		inwardInventory.setAdditionalInfo(iiData.getAdditionalComments());
		inwardInventory.setSupplier(supplierRepo.findById(iiData.getSupplierId()).get());
		inwardInventory.setWarehouse(warehouseRepo.findById(iiData.getWarehouseId()).get());
		inwardInventory.setInwardOutwardList(fetchInwardOutwardList(iiData.getProductWithQuantities(),
				warehouseRepo.findById(iiData.getWarehouseId()).get()));
		inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
	}

	public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities,
			Warehouse warehouse)
	{
		Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
		for (ProductWithQuantity productWithQuantity : productWithQuantities)
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
		for (ProductWithQuantity productWithQuantity : iiData.getProductWithQuantities())
		{
			if (!productRepo.existsById(productWithQuantity.getProductId()))
				throw new Exception("Product not found with ID " + productWithQuantity.getProductId());
			if (productWithQuantity.getQuantity() <= 0)
				throw new Exception("Quantity cannot be less that or equals zero");
		}
		if (!supplierRepo.existsById(iiData.getSupplierId()))
			throw new Exception("Supplier not found with ID");
		if (!warehouseRepo.existsById(iiData.getWarehouseId()))
			throw new Exception("Warehouse not found");

		Long duplicateProductIdCount = iiData.getProductWithQuantities().stream()
				.collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
				.filter(e -> e.getValue() > 1).count();

		if (duplicateProductIdCount > 0)
			throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");
	}

	public ReturnInwardInventoryData fetchInwardnventory(FilterDataList filterDataList, Pageable pageable)
			throws ParseException
	{
		ReturnInwardInventoryData returnInwardInventoryData = new ReturnInwardInventoryData();
		// Fetch Specification
		Specification<InwardInventory> spec = InwardInventorySpecification.getSpecification(filterDataList);

		// Feed listing
		if (spec != null)
			returnInwardInventoryData.setInwardInventory(inwardInventoryRepo.findAll(spec, pageable));
		else
			returnInwardInventoryData.setInwardInventory(inwardInventoryRepo.findAll(pageable));

		// Feed dropdowns
		returnInwardInventoryData.setIiDropdown(populateDropdownService.fetchData("inward"));

		// Feed totals
		returnInwardInventoryData.setTotals(spec != null ? fetchGroupingForFilteredData(spec, InwardInventory.class)
				: fetchInwardnventoryGroupBy());
		return returnInwardInventoryData;
	}

	public List<InwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList) throws Exception
	{
		Specification<InwardInventory> spec = InwardInventorySpecification.getSpecification(filterDataList);
		long size = spec != null ? inwardInventoryRepo.count(spec) : inwardInventoryRepo.count();
		System.out.println("Size of inward inventory after filter -" + size);
		if (size > 2000)
			throw new Exception("Too many rows to export. Apply some more filters and try again");
		System.out.println("Fetching data from db");
		List<InwardInventory> iiData = spec != null ? inwardInventoryRepo.findAll(spec) : inwardInventoryRepo.findAll();
		List<InwardInventoryExportDAO2> clonedData = transformDataForExport(iiData);
		System.out.println("Completed - returning to controller");
		return clonedData;
	}

	public List<ProductGroupedDAO> fetchInwardnventoryGroupBy() throws ParseException
	{
		List<ProductGroupedDAO> groupedData = inwardInventoryRepo.findGroupByInfo();
		return groupedData;
	}

	private List<ProductGroupedDAO> fetchGroupingForFilteredData(Specification<InwardInventory> spec,
			Class<InwardInventory> class1)
	{
		List<ProductGroupedDAO> groupedData = groupBySpecification.findDataByConfiguration(spec, InwardInventory.class,
				InwardInventory_.INWARD_OUTWARD_LIST);
		return groupedData;
	}

	private List<InwardInventoryExportDAO2> transformDataForExport(List<InwardInventory> iiData)
	{
		List<InwardInventoryExportDAO2> transformedData = new ArrayList<InwardInventoryExportDAO2>();
		for (InwardInventory ii : iiData)
		{
			for (InwardOutwardList ioList : ii.getInwardOutwardList())
			{
				InwardInventoryExportDAO2 ied = new InwardInventoryExportDAO2(ii, ioList);
				transformedData.add(ied);
			}
		}
		return transformedData;
	}

	public InwardInventory findById(long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if (inwardInventoryOpt.isPresent())
			return inwardInventoryOpt.get();
		else
			throw new Exception("Inward inventory not found");
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteInwardInventoryById(Long id) throws Exception
	{
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if (!inwardInventoryOpt.isPresent())
			throw new Exception("Inward Inventory with ID not found");
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		updateStockBeforeDelete(inwardInventory);
		removeOrphans(inwardInventory);
		inwardInventoryRepo.softDeleteById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	private void removeOrphans(InwardInventory inwardInventory)
	{
		Set<InwardOutwardList> iolList = inwardInventory.getInwardOutwardList();
		for (InwardOutwardList iol : iolList)
		{
			iol.setDeleted(true);
			iolRepo.save(iol);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	private void updateStockBeforeDelete(InwardInventory inwardInventory) throws Exception
	{
		String warehouseName = inwardInventory.getWarehouse().getWarehouseName();
		for (InwardOutwardList ioList : inwardInventory.getInwardOutwardList())
		{
			Double stock = ioList.getQuantity();
			Double currentStock = stockRepo
					.findStockForProductAndWarehouse(ioList.getProduct().getProductId(), warehouseName).get(0)
					.getQuantityInHand();
			if (currentStock < stock)
				throw new Exception("Cannot Delete. Stock will go negative if deleted");
			stockService.updateStock(ioList.getProduct().getProductId(), warehouseName, stock, "outward");
		}
	}

	@Transactional
	public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception
	{
		logger.info("In undate inward inventory flow");
		Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
		if (!inwardInventoryOpt.isPresent())
			throw new Exception("Inventory Entry with ID not found");
		validateInputs(iiData);
		InwardInventory inwardInventory = inwardInventoryOpt.get();
		InwardInventory oldInwardInventory = (InwardInventory) inwardInventory.clone();
		setFields(inwardInventory, iiData);
		modifyStockBeforeUpdate(oldInwardInventory, inwardInventory);
		removeOrphans(oldInwardInventory);
		return inwardInventoryRepo.save(inwardInventory);

	}

	@Transactional
	private void modifyStockBeforeUpdate(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
			throws Exception
	{
		if (!oldInwardInventory.getWarehouse().getWarehouseId().equals(inwardInventory.getWarehouse().getWarehouseId()))
			updateWhenWarehouseChanged(oldInwardInventory, inwardInventory);
		else
			updateWhenWarehouseSame(oldInwardInventory, inwardInventory);
	}

	@Transactional
	private void updateWhenWarehouseSame(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
			throws Exception
	{
		// Fetch product only in old and only in new and common
		Set<Long> oldProductSet = new HashSet<>(oldInwardInventory.getInwardOutwardList().size());
		Set<Long> newProductSet = new HashSet<>(inwardInventory.getInwardOutwardList().size());
		oldInwardInventory.getInwardOutwardList().stream().filter(p -> oldProductSet.add(p.getProduct().getProductId()))
				.collect(Collectors.toList());
		inwardInventory.getInwardOutwardList().stream().filter(p -> newProductSet.add(p.getProduct().getProductId()))
				.collect(Collectors.toList());
		Set<Long> onlyInOld = ReusableMethods.differenceBetweenSets(oldProductSet, newProductSet);
		Set<Long> onlyInNew = ReusableMethods.differenceBetweenSets(newProductSet, oldProductSet);
		Set<Long> commonInBoth = ReusableMethods.commonBetweenSets(oldProductSet, newProductSet);
		updateStockForOnlyInOld(onlyInOld, oldInwardInventory);
		updateStockForOnlyInNew(onlyInNew, inwardInventory);
		updateStockForCommonInBoth(commonInBoth, oldInwardInventory, inwardInventory);
	}

	@Transactional
	private void updateStockForCommonInBoth(Set<Long> commonInBoth, InwardInventory oldInwardInventory,
			InwardInventory inwardInventory) throws Exception
	{
		logger.info("Updating stock for productid common in both old and new");
		Set<InwardOutwardList> oldIOListSet = oldInwardInventory.getInwardOutwardList();
		Set<InwardOutwardList> newIOListSet = inwardInventory.getInwardOutwardList();
		for (Long id : commonInBoth)
		{
			logger.info("Updating stock for productid -" + id);
			Double oldQuantity = findQuantityForProductInIOList(id, oldIOListSet);
			Double newQuantity = findQuantityForProductInIOList(id, newIOListSet);
			Double quantityForUpdate = newQuantity - oldQuantity;
			logger.info("Difference in quantity -" + quantityForUpdate);
			for (InwardOutwardList ioList : newIOListSet)
			{
				logger.info("Processing list item " + ioList.getEntryid() + " from new list");
				logger.info("Comparing product item from list " + ioList.getProduct().getProductId()
						+ " with common product id -" + id);
				logger.info("Old Closing stock for product - " + ioList.getProduct().getProductId() + " is - "
						+ ioList.getClosingStock());
				if (id.equals(ioList.getProduct().getProductId()))
				{
					logger.info("Quantity modified for product - " + ioList.getProduct().getProductId());
					Double closingStock = stockService.updateStock(id,
							inwardInventory.getWarehouse().getWarehouseName(), quantityForUpdate, "inward");
					logger.info("Updated Closing stock - " + closingStock + " for product - " + id);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							inwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
					ioList.setClosingStock(closingStock);
				}
			}
			inwardInventory.setInwardOutwardList(newIOListSet);
		}

	}

	@Transactional
	private Double findQuantityForProductInIOList(Long productId, Set<InwardOutwardList> ioListSet)
	{
		for (InwardOutwardList ioList : ioListSet)
		{
			if (productId.equals(ioList.getProduct().getProductId()))
			{
				Double oldQuantity = ioList.getQuantity();
				return oldQuantity;
			}
		}
		return null;
	}

	@Transactional
	private void updateStockForOnlyInNew(Set<Long> onlyInNew, InwardInventory inwardInventory) throws Exception
	{
		logger.info("Processing Update Stock for productids present only in new");
		for (Long id : onlyInNew)
		{
			logger.info("Processing productid -" + id);
			Set<InwardOutwardList> ioListSet = inwardInventory.getInwardOutwardList();

			for (InwardOutwardList ioList : ioListSet)
			{
				logger.info("Processing entryid -" + ioList.getEntryid());
				logger.info("old closing stock - " + ioList.getClosingStock());
				if (id.equals(ioList.getProduct().getProductId()))
				{
					logger.info("Processing Element in old list but not in new - " + id);
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id,
							inwardInventory.getWarehouse().getWarehouseName(), quantity, "inward");
					System.out.println("Closing stock - " + closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							inwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
					ioList.setClosingStock(closingStock);
				}
			}
			inwardInventory.setInwardOutwardList(ioListSet);
		}

	}

	@Transactional
	private void updateStockForOnlyInOld(Set<Long> onlyInOld, InwardInventory oldInwardInventory) throws Exception
	{
		// Delete stock received as part of old inventory
		for (Long id : onlyInOld)
		{
			Set<InwardOutwardList> ioListSet = oldInwardInventory.getInwardOutwardList();
			for (InwardOutwardList ioList : ioListSet)
			{
				if (id.equals(ioList.getProduct().getProductId()))
				{
					System.out.println("Element in old list but not in new - " + id);
					Double quantity = ioList.getQuantity();
					Double closingStock = stockService.updateStock(id,
							oldInwardInventory.getWarehouse().getWarehouseName(), quantity, "outward");

					System.out.println("Closing stock - " + closingStock);
					inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
							oldInwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
				}
			}
		}
	}

	@Transactional
	private void updateWhenWarehouseChanged(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
			throws Exception
	{
		// Delete all stock added as part of old warehouse
		traverseListAndUpdateStock(oldInwardInventory.getInwardOutwardList(), "outward",
				oldInwardInventory.getWarehouse());

		// Add new stock to new warehouse
		Set<InwardOutwardList> newLIOList = traverseListAndUpdateStock(inwardInventory.getInwardOutwardList(), "inward",
				inwardInventory.getWarehouse());
		inwardInventory.setInwardOutwardList(newLIOList);
	}

	@Transactional
	private Set<InwardOutwardList> traverseListAndUpdateStock(Set<InwardOutwardList> ioListset, String type,
			Warehouse warehouse) throws Exception
	{
		for (InwardOutwardList oiList : ioListset)
		{
			Double closingStock = stockService.updateStock(oiList.getProduct().getProductId(),
					warehouse.getWarehouseName(), oiList.getQuantity(), type);
			inventoryNotificationService.pushQuantityEditedNotification(oiList.getProduct(),
					warehouse.getWarehouseName(), "inward", closingStock);
			oiList.setClosingStock(closingStock);
		}
		return ioListset;
	}
}
