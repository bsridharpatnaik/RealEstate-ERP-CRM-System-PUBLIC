package com.ec.application.service;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.ReusableClasses.*;
import com.ec.application.config.ProjectConstants;
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

import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockSpecification;

@Service
@Transactional(rollbackFor = Exception.class)
public class StockService {
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

    @Autowired
    StockInformationRepo siRepo;

    Logger log = LoggerFactory.getLogger(StockService.class);

    public StockInformationV2 fetchStockInformation(Pageable page, FilterDataList filterDataList) throws ParseException {
        StockInformationV2 stockInformation = new StockInformationV2();

        Boolean isHistorical = checkIfHistorical(filterDataList);
        if (!isHistorical) {
            Specification<StockInformationFromView> spec = StockInformationSpecification.getSpecification(filterDataList);
            Page<StockInformationDTO> map;
            if (spec == null)
                map = siRepo.findAll(page).map(this::convertToDTO);
            else
                map = siRepo.findAll(spec, page).map(this::convertToDTO);
            stockInformation.setStockInformation(map);
            return stockInformation;
        } else {
            return getHistoricalData(page, filterDataList);
        }
    }

    private StockInformationV2 getHistoricalData(Pageable page, FilterDataList filterDataList) throws ParseException {
        StockInformationV2 returnData = new StockInformationV2();
        String closingDateStr = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "closingDate").get(0);
        Date closingDate = ReusableMethods.atEndOfDay((new SimpleDateFormat(ProjectConstants.dateFormat).parse(closingDateStr)));
        List<StockInformationFromView> dbData = siRepo.getHistoricalStock(closingDate);
        List<StockInformationFromView> filteredData = filterStockInformation(dbData, filterDataList);
        returnData = convertToPageAndSort(filteredData, page);
        return returnData;
    }

    private StockInformationV2 convertToPageAndSort(List<StockInformationFromView> filteredData, Pageable page) {
        StockInformationV2 returnData = new StockInformationV2();
        filteredData = sortStockInformationsList(filteredData, page.getSort());
        Page<StockInformationFromView> pagedData = convertListStockToPages(filteredData, page);
        returnData.setStockInformation(pagedData.map(this::convertToDTO));
        return returnData;
    }

    private Page<StockInformationFromView> convertListStockToPages(List<StockInformationFromView> stockInformationsList,
                                                                   Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        int start = (int) pageable.getOffset();
        int end = (start + pageable.getPageSize()) > stockInformationsList.size() ? stockInformationsList.size(): (start + pageable.getPageSize());
        stockInformationsList = sortStockInformationsList(stockInformationsList, pageable.getSort());
        return new PageImpl<StockInformationFromView>(stockInformationsList.subList(start, end), pageable,
                stockInformationsList.size());
    }

    private List<StockInformationFromView> sortStockInformationsList(List<StockInformationFromView> stockInformationsList, Sort sort) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        try {
            String[] sortBy = sort.toString().split(":");
            String field = sortBy[0].trim();
            String order = sortBy[1].trim();
            switch (field) {
                case "productId":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator
                                .comparing(StockInformationFromView::getProductId, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getProductId,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
                case "productName":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getProductName,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getProductName,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
                case "categoryName":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getCategoryName,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getCategoryName,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
                case "totalQuantityInHand":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getTotalQuantityInHand,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getTotalQuantityInHand,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
                case "reorderQuantity":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getReorderQuantity,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getReorderQuantity,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
                case "stockStatus":
                    if (order.toLowerCase().contains("desc"))
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getStockStatus,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    else
                        stockInformationsList.sort(Comparator.comparing(StockInformationFromView::getStockStatus,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
                    break;
            }
            return stockInformationsList;
        } catch (Exception e) {
            System.out.println("Sorting failed");
            return stockInformationsList;
        }
    }

    private List<StockInformationFromView> filterStockInformation(List<StockInformationFromView> dbData, FilterDataList filterDataList) {
        List<StockInformationFromView> filteredData = new ArrayList<StockInformationFromView>();
        for (FilterAttributeData filterData : filterDataList.getFilterData()) {
            if (filterData.getAttrName().equalsIgnoreCase("products")) {
                List<String> products = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
                dbData = dbData.stream().filter(c -> products.contains(c.getProductName())).collect(Collectors.toList());
            }
            if (filterData.getAttrName().equalsIgnoreCase("categories")) {
                List<String> categories = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
                dbData = dbData.stream().filter(c -> categories.contains(c.getCategoryName())).collect(Collectors.toList());
            }
            if (filterData.getAttrName().equalsIgnoreCase("stockStatus")) {
                List<String> stockStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stockStatus");
                if (!stockStatus.contains("All")) {
                    dbData = dbData.stream().filter(c -> stockStatus.contains(c.getStockStatus())).collect(Collectors.toList());
                }
            }
        }
        return dbData;
    }

    private Boolean checkIfHistorical(FilterDataList filterDataList) {
        Boolean isHistorical = false;
        for (FilterAttributeData fa : filterDataList.getFilterData()) {
            if (fa.getAttrName().equals("closingDate")) {
                isHistorical = true;
                break;
            }
        }
        return isHistorical;
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
        	System.out.println(e);
            return null;
        }
    }

    public NameAndProjectionDataForDropDown getStockDropdownValues() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return populateDropdownService.fetchData("stock");
    }

    public List<StockInformationExportDAO> findStockForAllForExport(FilterDataList filterDataList) throws Exception {
        StockInformationV2 fetchStockInformation = fetchStockInformation(PageRequest.of(0, Integer.MAX_VALUE), filterDataList);
        List<StockInformationExportDAO> exportData = new ArrayList<StockInformationExportDAO>();
        for (StockInformationDTO dto : fetchStockInformation.getStockInformation()) {
            for (SingleStockInformationDTO sInfo : dto.getDetailedStock()) {
                StockInformationExportDAO si = new StockInformationExportDAO();
                si.setWarehouseStock(sInfo.getQuantityInHand());
                si.setTotalStock(dto.getTotalQuantityInHand().toString());
                si.setWarehouse(sInfo.getWarehouseName());
                si.setInventory(dto.getProductName());
                si.setCategory(dto.getCategoryName());
                si.setProductId(dto.getProductId());
                si.setMeasurementUnit(dto.getMeasurementUnit());
                si.setStockStatus(dto.getStockStatus());
                si.setReorderQuantity(dto.getReorderQuantity());
                exportData.add(si);
            }
        }
        return exportData;
    }


    private List<StockInformationExportDAO> transformDataForExport(Page<SingleStockInfo> allStocks) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<StockInformationExportDAO> stockInformationExportDAO = new ArrayList<StockInformationExportDAO>();
        for (SingleStockInfo ssi : allStocks) {
            for (Stock stock : ssi.getDetailedStock()) {
                stockInformationExportDAO.add(new StockInformationExportDAO(ssi, stock));
            }
        }
        return stockInformationExportDAO;
    }

    @Transactional(rollbackFor = Exception.class)
    public Double updateStock(Long productId, String warehousename, Double quantity, String operation) throws Exception {
        Stock currentStock = findOrInsertStock(productId, warehousename);
        Double oldStock = currentStock.getQuantityInHand();
        Double newStock = (double) 0;
        switch (operation) {
            case "inward":
                newStock = oldStock + quantity;
                break;
            case "outward":
                newStock = oldStock - quantity;
        }
        if (newStock < 0) {
            log.info("stock update failed for product " + currentStock.getProduct().getProductName()
                    + ".  Stock will go Negative");
            throw new Exception("stock update failed for product " + currentStock.getProduct().getProductName()
                    + ".  Stock will go Negative");
        } else {
            currentStock.setQuantityInHand(newStock);

            stockRepo.save(currentStock);
            inventoryNotificationService.checkStockAndPushLowStockNotification(currentStock.getProduct());
            return newStock;
        }

    }

    @Transactional(rollbackFor = Exception.class)
    private Stock findOrInsertStock(Long productId, String warehousename) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

        Optional<Product> productOpt = productRepo.findById(productId);
        List<Warehouse> warehouseOpt = warehouseRepo.findByName(warehousename);

        if (!productOpt.isPresent() || warehouseOpt.size() != 1)
            throw new Exception("Product of warehouse not found");

        Product product = productOpt.get();
        Warehouse warehouse = warehouseOpt.get(0);
        List<Stock> stocks = stockRepo.findByIdName(productId, warehousename);
        if (stocks.size() == 0) {
            Stock stock = new Stock(product, warehouse, 0.0);
            return stockRepo.save(stock);
        } else {
            return stocks.get(0);
        }
    }


    public Double findStockForProductWarehouse(Long productId, Long warehouseId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Double currentStock = stockRepo.getCurrentStockForProductWarehouse(productId, warehouseId);
        return currentStock;
    }

    public Double findTotalStockForProduct(Long productId) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Double currentStock = stockRepo.getCurrentTotalStockForProduct(productId);
        return currentStock;
    }

    public List<ProductIdAndStockProjection> findStockForProductListWarehouse(CurrentStockRequest currentStockRequest) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<Long> productIds = currentStockRequest.getProductIds();
        Long warehouseId = currentStockRequest.getWarehouseId();
        List<ProductIdAndStockProjection> stockInfo = stockRepo.getCurrentStockForProductListWarehouse(productIds,
                warehouseId);
        List<Long> returnedProductIds = stockInfo.stream().map(ProductIdAndStockProjection::getProductId)
                .collect(Collectors.toList());
        productIds.removeAll(returnedProductIds);
        for (Long productId : productIds) {
            ProductIdAndStockProjection productsWarehouseStockProjection = new ProductIdAndStockProjection(productId,
                    (double) 0);
            stockInfo.add(productsWarehouseStockProjection);
        }
        return stockInfo;
    }

    public List<StockPercentData> fetchStockPercent() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<StockPercentData> all = stockRepo.getCurrentStockPercent();
        Comparator<StockPercentData> NameCommparator = Comparator.comparing(StockPercentData::getUpdated);
        List<StockPercentData> sorted = all.stream().sorted(NameCommparator).filter(c -> c.getStockPercent() < 120)
                .limit(20).collect(Collectors.toList());

        return sorted;
    }

    public void sendStockNotificationEmail() throws Exception {

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

    public void sendStockValidationEmail() throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<StockValidation> data = stockValidationRepo.findAll(Sort.by(Sort.Direction.ASC, "inventory"));
        if (data.size() > 0)
            emailHelper.sendEmailForStockValidation(data);
        else
            log.info("Stock Validation Successful. Skipping email.");
    }

    public void deleteStockForProduct(Long id) throws Exception {
        Page<Stock> stockList = stockRepo.findStockForProduct(PageRequest.of(0, Integer.MAX_VALUE),id);
        List<Stock> stocksToBeDeleted = new ArrayList<>();
        for (Stock stock : stockList)
        {
            if(stock.getQuantityInHand()>0)
                throw new Exception("Cannot delete Stock/Product. Contact Administrator");
            stockRepo.softDelete(stock);
        }
    }
}
