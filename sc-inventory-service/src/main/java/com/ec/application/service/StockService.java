package com.ec.application.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.EmailHelper;
import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.data.CurrentStockRequest;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.data.SingleStockInfo;
import com.ec.application.data.StockInformation;
import com.ec.application.data.StockInformationExportDAO;
import com.ec.application.data.StockPercentData;
import com.ec.application.model.Product;
import com.ec.application.model.Stock;
import com.ec.application.model.Warehouse;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.StockValidationRepo;
import com.ec.application.repository.WarehouseRepo;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockSpecification;

@Service
@Transactional(rollbackFor = Exception.class)
public class StockService
{
	@Autowired
	StockRepo stockRepo;

	@Autowired
	ProductRepo productRepo;

	@Autowired
	ProductService productService;

	@Autowired
	WarehouseRepo warehouseRepo;

	@Autowired
	PopulateDropdownService populateDropdownService;

	@Autowired
	WarehouseService warehouseService;

	@Autowired
	EmailHelper emailHelper;

	@Autowired
	StockService stockService;

	@Autowired
	StockHistoryService stockHistoryService;

	@Autowired
	StockValidationRepo stockValidationRepo;

	@Autowired
	InventoryNotificationService inventoryNotificationService;

	Logger log = LoggerFactory.getLogger(StockService.class);

	public StockInformation findStockForAll(FilterDataList filterDataList, Pageable pageable) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<Stock> spec = StockSpecification.getSpecification(filterDataList);
		List<Stock> allStocks = new ArrayList<Stock>();
		if (spec != null)
			allStocks = stockRepo.findAll(spec);
		else
			allStocks = stockRepo.findAll();
		String showLowOrHIgh = fetchFilterForStockStatus(filterDataList);
		return fetchStockInformation(allStocks, pageable, showLowOrHIgh);
	}

	private String fetchFilterForStockStatus(FilterDataList filterDataList)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		String showLowOrHIgh = "All";
		List<String> stockStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stockStatus");
		if (stockStatus != null)
			if (stockStatus.get(0).toLowerCase().equals("high"))
				showLowOrHIgh = "High";
			else if (stockStatus.get(0).toLowerCase().equals("low"))
				showLowOrHIgh = "Low";
		return showLowOrHIgh;
	}

	public NameAndProjectionDataForDropDown getStockDropdownValues()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return populateDropdownService.fetchData("stock");
	}

	public List<StockInformationExportDAO> findStockForAllForExport(FilterDataList filterDataList) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<Stock> spec = StockSpecification.getSpecification(filterDataList);
		long size = spec != null ? stockRepo.count(spec) : stockRepo.count();
		if (size > 2000)
			throw new Exception("Too many rows to export. Apply some more filters and try again");

		List<Stock> allStocks = spec != null ? stockRepo.findAll(spec) : stockRepo.findAll();
		Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.Direction.ASC, "productName");
		String showLowOrHIgh = fetchFilterForStockStatus(filterDataList);
		Page<SingleStockInfo> stockInfoList = fetchStockInformation(allStocks, pageable, showLowOrHIgh)
				.getStockInformation();
		List<StockInformationExportDAO> exportData = transformDataForExport(stockInfoList);
		return exportData;
	}

	private List<StockInformationExportDAO> transformDataForExport(Page<SingleStockInfo> allStocks)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<StockInformationExportDAO> stockInformationExportDAO = new ArrayList<StockInformationExportDAO>();
		for (SingleStockInfo ssi : allStocks)
		{
			for (Stock stock : ssi.getDetailedStock())
			{
				stockInformationExportDAO.add(new StockInformationExportDAO(ssi, stock));
			}
		}
		return stockInformationExportDAO;
	}

	@Transactional(rollbackFor = Exception.class)
	public Double updateStock(Long productId, String warehousename, Double quantity, String operation) throws Exception
	{
		Stock currentStock = findOrInsertStock(productId, warehousename);
		Double oldStock = currentStock.getQuantityInHand();
		Double newStock = (double) 0;
		switch (operation)
		{
		case "inward":
			newStock = oldStock + quantity;
			break;
		case "outward":
			newStock = oldStock - quantity;
		}
		if (newStock < 0)
		{
			log.info("stock update failed for product " + currentStock.getProduct().getProductName()
					+ ".  Stock will go Negative");
			throw new Exception("stock update failed for product " + currentStock.getProduct().getProductName()
					+ ".  Stock will go Negative");
		} else
		{
			currentStock.setQuantityInHand(newStock);

			stockRepo.save(currentStock);
			inventoryNotificationService.checkStockAndPushLowStockNotification(currentStock.getProduct());
			return newStock;
		}

	}

	@Transactional(rollbackFor = Exception.class)
	private Stock findOrInsertStock(Long productId, String warehousename) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		Optional<Product> productOpt = productRepo.findById(productId);
		List<Warehouse> warehouseOpt = warehouseRepo.findByName(warehousename);

		if (!productOpt.isPresent() || warehouseOpt.size() != 1)
			throw new Exception("Product of warehouse not found");

		Product product = productOpt.get();
		Warehouse warehouse = warehouseOpt.get(0);
		List<Stock> stocks = stockRepo.findByIdName(productId, warehousename);
		if (stocks.size() == 0)
		{
			Stock stock = new Stock(product, warehouse, 0.0);
			return stockRepo.save(stock);
		} else
		{
			return stocks.get(0);
		}
	}

	public StockInformation fetchStockInformation(List<Stock> allStocks, Pageable pageable, String showLowOrHIgh)
			throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		StockInformation stockInformation = new StockInformation();
		List<SingleStockInfo> stockInformationsList = new ArrayList<SingleStockInfo>();
		List<Long> uniqueProductIds = fetchUniqueProductIds(allStocks);
		for (Long productId : uniqueProductIds)
		{
			Double currentStock = stockRepo.getTotalStockForProduct(productId);
			DecimalFormat df = new DecimalFormat("###.#");
			Product product = productService.findSingleProduct(productId);
			SingleStockInfo singleStockInfo = new SingleStockInfo();
			singleStockInfo.setProductId(productId);
			singleStockInfo.setProductName(product.getProductName());
			singleStockInfo.setCategoryName(product.getCategory().getCategoryName());
			singleStockInfo.setTotalQuantityInHand(df.format(currentStock));
			singleStockInfo.setDetailedStock(findStockForProductAsList(productId, allStocks));
			singleStockInfo.setReorderQuantity(product.getReorderQuantity());
			if (currentStock < product.getReorderQuantity())
				singleStockInfo.setStockStatus("Low");
			else
				singleStockInfo.setStockStatus("High");
			stockInformationsList.add(singleStockInfo);
		}

		List<SingleStockInfo> filteredStockInformationsList = filterStockForLowStock(stockInformationsList,
				showLowOrHIgh);
		stockInformation.setStockInformation(convertListStockToPages(filteredStockInformationsList, pageable));
		return stockInformation;
	}

	private List<SingleStockInfo> filterStockForLowStock(List<SingleStockInfo> stockInformationsList,
			String showLowOrHIgh)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		List<SingleStockInfo> filteredStockListForLowStock = new ArrayList<SingleStockInfo>();
		if (showLowOrHIgh != null)
		{
			if (showLowOrHIgh.equals("Low"))
			{
				filteredStockListForLowStock = stockInformationsList.stream().filter(c -> c.getStockStatus() == "Low")
						.collect(Collectors.toList());
			} else if (showLowOrHIgh.equals("High"))
			{
				filteredStockListForLowStock = stockInformationsList.stream().filter(c -> c.getStockStatus() == "High")
						.collect(Collectors.toList());
			} else
				filteredStockListForLowStock = stockInformationsList;
		} else
			filteredStockListForLowStock = stockInformationsList;
		return filteredStockListForLowStock;
	}

	private Page<SingleStockInfo> convertListStockToPages(List<SingleStockInfo> stockInformationsList,
			Pageable pageable)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		int start = (int) pageable.getOffset();
		int end = (start + pageable.getPageSize()) > stockInformationsList.size() ? stockInformationsList.size()
				: (start + pageable.getPageSize());
		stockInformationsList = sortStockInformationsList(stockInformationsList, pageable.getSort());
		return new PageImpl<SingleStockInfo>(stockInformationsList.subList(start, end), pageable,
				stockInformationsList.size());
	}

	private List<SingleStockInfo> sortStockInformationsList(List<SingleStockInfo> stockInformationsList, Sort sort)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		try
		{
			String[] sortBy = sort.toString().split(":");
			String field = sortBy[0].trim();
			String order = sortBy[1].trim();

			switch (field)
			{
			case "productId":
				if (order.toLowerCase().contains("desc"))
					stockInformationsList.sort(Comparator
							.comparing(SingleStockInfo::getProductId, Comparator.nullsFirst(Comparator.naturalOrder()))
							.reversed());
				else
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductId,
							Comparator.nullsFirst(Comparator.naturalOrder())));
				break;
			case "productName":
				if (order.toLowerCase().contains("desc"))
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductName,
							Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
				else
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getProductName,
							Comparator.nullsFirst(Comparator.naturalOrder())));
				break;
			case "categoryName":
				if (order.toLowerCase().contains("desc"))
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getCategoryName,
							Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
				else
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getCategoryName,
							Comparator.nullsFirst(Comparator.naturalOrder())));
				break;
			case "totalQuantityInHand":
				if (order.toLowerCase().contains("desc"))
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getTotalQuantityInHand,
							Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
				else
					stockInformationsList.sort(Comparator.comparing(SingleStockInfo::getTotalQuantityInHand,
							Comparator.nullsFirst(Comparator.naturalOrder())));
				break;
			}
			return stockInformationsList;
		} catch (Exception e)
		{
			System.out.println("Sorting failed");
			return stockInformationsList;
		}
	}

	private List<Stock> findStockForProductAsList(Long productId, List<Stock> allStocks)
	{

		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<Stock> stocks = allStocks.stream().filter(p -> p.getProduct().getProductId() == productId)
				.collect(Collectors.toList());
		return stocks;
	}

	private List<Long> fetchUniqueProductIds(List<Stock> allStocks)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return allStocks.stream().map(p -> p.getProduct().getProductId()).distinct().collect(Collectors.toList());
	}

	public Double findStockForProductWarehouse(Long productId, Long warehouseId)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Double currentStock = stockRepo.getCurrentStockForProductWarehouse(productId, warehouseId);
		return currentStock;
	}

	public Double findTotalStockForProduct(Long productId)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Double currentStock = stockRepo.getCurrentTotalStockForProduct(productId);
		return currentStock;
	}

	public List<ProductIdAndStockProjection> findStockForProductListWarehouse(CurrentStockRequest currentStockRequest)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<Long> productIds = currentStockRequest.getProductIds();
		Long warehouseId = currentStockRequest.getWarehouseId();
		List<ProductIdAndStockProjection> stockInfo = stockRepo.getCurrentStockForProductListWarehouse(productIds,
				warehouseId);
		List<Long> returnedProductIds = stockInfo.stream().map(ProductIdAndStockProjection::getProductId)
				.collect(Collectors.toList());
		productIds.removeAll(returnedProductIds);
		for (Long productId : productIds)
		{
			ProductIdAndStockProjection productsWarehouseStockProjection = new ProductIdAndStockProjection(productId,
					(double) 0);
			stockInfo.add(productsWarehouseStockProjection);
		}
		return stockInfo;
	}

	public List<StockPercentData> fetchStockPercent()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return stockRepo.getCurrentStockPercent();
	}

	public void sendStockNotificationEmail() throws Exception
	{

		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		FilterDataList filterDataList = new FilterDataList();
		List<FilterAttributeData> filterData = new ArrayList<FilterAttributeData>();
		filterDataList.setFilterData(filterData);
		log.info("Fetching stock information");
		List<StockInformationExportDAO> dataForInsertList = stockService.findStockForAllForExport(filterDataList);

		log.info("Sending email for stock information");
		emailHelper.sendEmailForMorningStockNottification(dataForInsertList);
		log.info("saving stock information to DB");
		stockHistoryService.insertLatestStockHistory(dataForInsertList);
	}

	public void sendStockValidationEmail() throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		emailHelper.sendEmailForStockValidation(stockValidationRepo.findAll(Sort.by(Sort.Direction.ASC, "inventory")));
	}
}
