package com.ec.application.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.ReusableClasses.*;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;
import com.ec.application.Filters.StockInformationSpecification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
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

import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;

@Service
@Transactional(rollbackFor = Exception.class)
public class StockService {
    @Autowired
    StockRepo stockRepo;

    @Autowired
    StockReportRepository stockReportRepository;

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
    AllInventoryRepo allInventoryRepo;

    @Autowired
    StockInformationRepo siRepo;

    @Autowired
    ActiveProfileService activeProfileService;

    Logger log = LoggerFactory.getLogger(StockService.class);

    public StockInformationV2 fetchStockInformation(Pageable page, FilterDataList filterDataList) throws ParseException {
        StockInformationV2 stockInformation = new StockInformationV2();

        if (checkIfHistorical(filterDataList)) {
            return getHistoricalData(page, filterDataList);
        }

        Specification<StockInformationFromView> spec = StockInformationSpecification.getSpecification(filterDataList);
        Page<StockInformationFromView> list = (spec == null) ? siRepo.findAll(page) : siRepo.findAll(spec, page);

        // Fetch all ProductIds in the current page
        List<Long> productIds = list.stream()
                .map(StockInformationFromView::getProductId)
                .collect(Collectors.toList());

        // Fetch all InventoryTransactions in one go
        List<AllInventoryTransactions> allInventoryTransactions = allInventoryRepo.findInwardOutwardByProductIds(productIds);

        // Create a map of ProductId to List<AllInventoryTransactions>
        Map<Long, List<AllInventoryTransactions>> transactionsMap = allInventoryTransactions.stream()
                .collect(Collectors.groupingBy(AllInventoryTransactions::getProductId));

        // This wasted 5 hours for me. There can be records that have entry in stock but there may be zero inward/outward records. May be after adding inward, they deleted it.
        Page<StockInformationDTO> map = list.map(si -> convertToDTO(si, transactionsMap.get(si.getProductId()) == null ? new ArrayList<>() : transactionsMap.get(si.getProductId())));
        stockInformation.setStockInformation(map);
        return stockInformation;
    }

    private StockInformationV2 getHistoricalData(Pageable page, FilterDataList filterDataList) throws ParseException {
        StockInformationV2 returnData = new StockInformationV2();
        String closingDateStr = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "closingDate").get(0);
        Date closingDate = ReusableMethods.atEndOfDay((new SimpleDateFormat(ProjectConstants.dateFormat).parse(closingDateStr)));
        List<StockInformationFromView> dbData = siRepo.getHistoricalStock(closingDate);
        List<StockInformationFromView> filteredData = filterStockInformation(dbData, filterDataList);
        returnData = convertToPageAndSort(filteredData, page);
        updateStockAgingForHistorical(returnData, closingDate);
        return returnData;
    }

    private void updateStockAgingForHistorical(StockInformationV2 returnData, Date closingDate) {
        for (StockInformationDTO s : returnData.getStockInformation().getContent()) {
            for (SingleStockInformationDTO si : s.getDetailedStock()) {
                si.setStockAgingData(null);
            }
            List<AllInventoryTransactions> aiList = s.getInwardOutwardHistory();
            List<AllInventoryTransactions> filtered = aiList.stream().filter(i -> i.getDate().before(closingDate)).collect(Collectors.toList());
            s.setInwardOutwardHistory(filtered);
            if (!filtered.isEmpty()) {
                List<AllInventoryTransactions> iList = filtered.stream().filter(i -> i.getType().equalsIgnoreCase("inward")).collect(Collectors.toList());
                Date lastInwardDate = !iList.isEmpty() ? aiList.get(0).getDate() : null;
                s.setLastInwardDate(lastInwardDate);
            }
        }
    }

    private StockInformationV2 convertToPageAndSort(List<StockInformationFromView> filteredData, Pageable page) {
        StockInformationV2 returnData = new StockInformationV2();

        // Step 1: Collect all ProductIds from the filtered data
        List<Long> productIds = filteredData.stream()
                .map(StockInformationFromView::getProductId)
                .collect(Collectors.toList());
        List<AllInventoryTransactions> allInventoryTransactions = allInventoryRepo.findInwardOutwardByProductIds(productIds);
        Map<Long, List<AllInventoryTransactions>> transactionsMap = allInventoryTransactions.stream()
                .collect(Collectors.groupingBy(AllInventoryTransactions::getProductId));
        Page<StockInformationFromView> convertedList = convertListStockToPages(sortStockInformationsList(filteredData, page.getSort()), page);
        returnData.setStockInformation(convertedList.map(si -> convertToDTO(si, transactionsMap.get(si.getProductId()))));
        return returnData;
    }


    private Page<StockInformationFromView> convertListStockToPages(List<StockInformationFromView> stockInformationsList,
                                                                   Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), stockInformationsList.size());
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


    private StockInformationDTO convertToDTO(StockInformationFromView si, List<AllInventoryTransactions> aiList) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            StockInformationDTO dto = new StockInformationDTO();
            dto.setDetailedStock(mapper.readValue(si.getDetailedStock(), new TypeReference<List<SingleStockInformationDTO>>() {
            }));
            dto.updateDetailedStock(dto.getDetailedStock(), getStockAgingData(aiList, dto.getDetailedStock()));
            dto.setLastInwardDate(getLastInwardDate(aiList));
            dto.setCategoryName(si.getCategoryName());
            dto.setProductId(si.getProductId());
            dto.setStockStatus(si.getStockStatus());
            dto.setMeasurementUnit(si.getMeasurementUnit());
            dto.setProductName(si.getProductName());
            dto.setReorderQuantity(si.getReorderQuantity());
            dto.setTotalQuantityInHand(si.getTotalQuantityInHand());
            dto.setInwardOutwardHistory(aiList);
            return dto;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    private Date getLastInwardDate(List<AllInventoryTransactions> aiList) {
        List<AllInventoryTransactions> filtered = aiList.stream().filter(e -> e.getType().equalsIgnoreCase("inward")).collect(Collectors.toList());
        if (!filtered.isEmpty())
            return filtered.get(0).getDate();
        return null;
    }

    private StockAgingData getStockAgingData(List<AllInventoryTransactions> aiList, List<SingleStockInformationDTO> detailedStock) {
        StockAgingData stockAgingData = new StockAgingData();
        Map<String, List<StockAgeDTO>> stockMap = new HashMap<String, List<StockAgeDTO>>();
        for (SingleStockInformationDTO si : detailedStock) {
            if (si.getQuantityInHand() <= 0)
                continue;

            String warehouse = si.getWarehouseName();
            Double stock = si.getQuantityInHand();
            List<AllInventoryTransactions> aiListFIltered = aiList.stream().filter(ai -> ai.getType()
                            .equalsIgnoreCase("Inward") && ai.getWarehouseName()
                            .equalsIgnoreCase(warehouse))
                    .sorted(Comparator.comparing(AllInventoryTransactions::getId))
                    .collect(Collectors.toList());
            List<StockAgeDTO> stockAges = calculateStockAges(aiListFIltered, stock);
            stockMap.put(warehouse, stockAges);
        }
        stockAgingData.setStockAge(stockMap);
        return stockAgingData;
    }

    private List<StockAgeDTO> calculateStockAges(List<AllInventoryTransactions> aiList, Double currentStock) {
        List<StockAgeDTO> stockAges = new ArrayList<>();
        Double accumulatedQuantity = 0.0;
        Date today = new Date(); // Current date

        for (AllInventoryTransactions entry : aiList) {
            if (currentStock <= 0) break;

            Double quantity = entry.getQuantity();
            Date entryDate = entry.getDate();

            if (currentStock >= quantity) {
                stockAges.add(new StockAgeDTO(quantity, entryDate, daysBetween(entryDate, today)));
                currentStock -= quantity;
            } else {
                stockAges.add(new StockAgeDTO(currentStock, entryDate, daysBetween(entryDate, today)));
                currentStock = 0.0;
            }
        }
        return stockAges;
    }

    private static String daysBetween(Date startDate, Date endDate) {
        long differenceInMillis = endDate.getTime() - startDate.getTime();
        return convertDaysToWords((int) (differenceInMillis / (1000 * 60 * 60 * 24)));
    }

    public static String convertDaysToWords(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days cannot be negative");
        }

        int years = days / 365;
        days %= 365;

        int months = days / 30;
        days %= 30;

        int weeks = days / 7;
        days %= 7;

        StringBuilder result = new StringBuilder();

        if (years > 0) {
            result.append(years).append(" year").append(years > 1 ? "s" : "");
        }

        if (months > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(months).append(" month").append(months > 1 ? "s" : "");
        }

        if (weeks > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(weeks).append(" week").append(weeks > 1 ? "s" : "");
        }

        if (days > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(days).append(" day").append(days > 1 ? "s" : "");
        }

        String resultString = result.toString();

        // Handle the case where there are multiple units by adding "and" before the last unit
        int lastCommaIndex = resultString.lastIndexOf(", ");
        if (lastCommaIndex != -1) {
            resultString = resultString.substring(0, lastCommaIndex) + " and" + resultString.substring(lastCommaIndex + 1);
        }

        return resultString.isEmpty() ? "0 days" : resultString;
    }

    public NameAndProjectionDataForDropDown getStockDropdownValues() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return populateDropdownService.fetchData("stock");
    }

    public <T> List<T> findStockForAllForExport(FilterDataList filterDataList) throws Exception {
        if (activeProfileService.fetchProfile().contains("sc-")) {
            return (List<T>) stockReportRepository.findAll();
        } else {
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
                    si.setLastInwardDate(dto.getLastInwardDate());
                    exportData.add(si);
                }
            }
            return (List<T>) exportData;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Double updateStock(Long productId, Long warehouseId, Double quantity, String operation) throws Exception {
        System.out.println("updateStock - Tenant =- " + ThreadLocalStorage.getTenantName());
        Stock currentStock = findOrInsertStock(productId, warehouseId);
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
    private Stock findOrInsertStock(Long productId, Long warehouseId) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<Product> productOpt = productRepo.findById(productId);
        Optional<Warehouse> warehouseOpt = warehouseRepo.findById(warehouseId);

        if (!productOpt.isPresent() || !warehouseOpt.isPresent())
            throw new Exception("Product or warehouse not found");

        Product product = productOpt.get();
        Warehouse warehouse = warehouseOpt.get();
        List<Stock> stocks = stockRepo.findByProductAndWarehouseId(productId, warehouseId);
        if (stocks.isEmpty()) {
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

    public Double findStockForProductWarehouse(Long productId, String warehouseName) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Double currentStock = stockRepo.getCurrentStockForProductWarehouseByName(productId, warehouseName);
        return currentStock;
    }

    public Map<Long, Double> findStockForProductsInWarehouse(Long warehouseId, List<Long> productIds, String tenantName) throws Exception {

        if (warehouseId == null)
            throw new Exception("Warehouse ID cannot be null");


        if (productIds == null || productIds.size() == 0)
            throw new Exception("Product IDs cannot be null or empty");

        if(tenantName!=null)
            ThreadLocalStorage.setTenantName(tenantName);
        List<Object[]> results = stockRepo.getCurrentStockForProductsInWarehouse(warehouseId, productIds);
        Map<Long, Double> stockMap = new HashMap<>();
        for (Object[] row : results) {
            Long productId = (Long) row[0];
            Double stock = (Double) row[1];
            stockMap.put(productId, stock);
        }
        return stockMap;
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
        //List<StockInformationExportDAO> dataForInsertList = stockService.findStockForAllForExport(filterDataList);

        log.info("Sending email for stock information");
        //emailHelper.sendEmailForMorningStockNottification(dataForInsertList);
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
        Page<Stock> stockList = stockRepo.findStockForProduct(PageRequest.of(0, Integer.MAX_VALUE), id);
        List<Stock> stocksToBeDeleted = new ArrayList<>();
        for (Stock stock : stockList) {
            if (stock.getQuantityInHand() > 0)
                throw new Exception("Cannot delete Stock/Product. Contact Administrator");
            stockRepo.softDelete(stock);
        }
    }

    public List<Stock> getDeadStocks() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<Stock> deadStocks = stockRepo.findDeadStocks();
        return deadStocks;
    }
}
