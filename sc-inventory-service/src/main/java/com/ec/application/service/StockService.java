package com.ec.application.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.repository.*;
import com.ec.common.Filters.StockInformationSpecification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Autowired
	StockInformationRepo siRepo;

	public StockInformationV2 fetchStockInformation(Pageable page, FilterDataList filterDataList) {
		StockInformationV2 stockInformation = new StockInformationV2();

		Specification<StockInformationFromView> spec = StockInformationSpecification.getSpecification(filterDataList);
		final Page<StockInformationDTO> map;
		if (spec == null)
			map = siRepo.findAll(page).map(this::convertToDTO);
		else
			map = siRepo.findAll(spec, page).map(this::convertToDTO);

		stockInformation.setStockInformation(map);
		return stockInformation;
	}

	private StockInformationDTO convertToDTO(StockInformationFromView si) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			StockInformationDTO dto = new StockInformationDTO();
			dto.setDetailedStock(mapper.readValue(si.getDetailedStock(), new TypeReference<List<SingleStockInformationDTO>>() {
			}));
			dto.setCategoryName(si.getCategoryName());
			dto.setProductId(si.getProductId());
			dto.setStockStatus(si.getStockStatus());
			dto.setMeasurementUnit(si.getMeasurementUnit());
			dto.setProductName(si.getProductName());
			dto.setReorderQuantity(si.getReorderQuantity());
			dto.setTotalQuantityInHand(si.getTotalQuantityInHand());
			return dto;
		} catch (Exception e) {
			return null;
		}
	}

	public NameAndProjectionDataForDropDown getStockDropdownValues()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return populateDropdownService.fetchData("stock");
	}

	public List<StockInformationExportDAO> findStockForAllForExport(FilterDataList filterDataList) throws Exception
	{
		List<StockInformationExportDAO> exportData = new ArrayList<StockInformationExportDAO>();
		Specification<StockInformationFromView> spec = StockInformationSpecification.getSpecification(filterDataList);
		final List<StockInformationDTO> data;
		if (spec == null)
			data = siRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
		else
			data = siRepo.findAll(spec).stream().map(this::convertToDTO).collect(Collectors.toList());

		for(StockInformationDTO si : data) {
			for (SingleStockInformationDTO ssi : si.getDetailedStock()) {
				StockInformationExportDAO exDAO = new StockInformationExportDAO();
				exDAO.setCategory(si.getCategoryName());
				exDAO.setInventory(si.getProductName());
				exDAO.setTotalStock(si.getTotalQuantityInHand().toString());
				exDAO.setMeasurementUnit(si.getMeasurementUnit());
				exDAO.setProductId(si.getProductId());
				exDAO.setWarehouse(ssi.getWarehouseName());
				exDAO.setWarehouseStock(ssi.getQuantityInHand());
				exportData.add(exDAO);
			}
		}
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
		List<StockPercentData> all = stockRepo.getCurrentStockPercent();
		Comparator<StockPercentData> NameCommparator = Comparator.comparing(StockPercentData::getUpdated);
		List<StockPercentData> sorted = all.stream().sorted(NameCommparator).filter(c -> c.getStockPercent() < 120)
				.limit(20).collect(Collectors.toList());

		return sorted;
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
		//stockHistoryService.insertLatestStockHistory(dataForInsertList);
	}

	public void sendStockValidationEmail() throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<StockValidation> data = stockValidationRepo.findAll(Sort.by(Sort.Direction.ASC, "inventory"));
		if(data.size()>0)
			emailHelper.sendEmailForStockValidation(data);
		else
			log.info("Stock Validation Successful. Skipping email.");
	}
}
