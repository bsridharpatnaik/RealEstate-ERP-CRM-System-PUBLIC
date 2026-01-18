package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.data.*;
import com.ec.application.indentpo.IndentReconciliationTaskService;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.InwardInventorySpecification;

@Service
@Transactional
public class InwardInventoryService {

    Logger logger = LoggerFactory.getLogger(InwardInventoryService.class);

    @Autowired
    InwardInventoryRepo inwardInventoryRepo;

    @Autowired
    private AsyncService asyncService;

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

    @Autowired
    UserDetailsService userDetailService;

    @Autowired
    AsyncServiceInventory asyncServiceInventory;

    @Autowired
    ProjectConstantsService projectConstantsService;

    @Autowired
    IndentsForInwardViewRepository indentsForInwardViewRepository;

    @Autowired
    TenantService tenantService;

    @Autowired
    IndentReconciliationTaskService indentReconciliationTaskService;

    @Autowired
    EditAuthorizationService editAuthorizationService;

    Logger log = LoggerFactory.getLogger(InwardInventoryService.class);

    public List<PoDropdownItem> getPendingPoDropdown() {
        String tenant = tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());
        List<Object[]> rows = indentsForInwardViewRepository.findPendingPoDropdown(IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, tenant);

        return rows.stream()
                .map(r -> {
                    PoDropdownItem dto = new PoDropdownItem();
                    dto.setPurchaseOrderNumber((String) r[0]);
                    dto.setPoDate((java.util.Date) r[1]);
                    dto.setSupplierName((String) r[2]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public PoForInwardResponse getPoForInward(String poNumber) {
        String tenant = tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName());
        List<IndentsForInwardView> rows = indentsForInwardViewRepository.findPendingLineItemsForPO(IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, poNumber, tenant);

        if (rows.isEmpty()) {
            throw new IllegalStateException("No pending inward items for this PO and tenant");
        }

        IndentsForInwardView first = rows.get(0);

        PoForInwardResponse response = new PoForInwardResponse();
        response.setPurchaseOrderNumber(first.getPurchaseOrderNumber());
        response.setPoDate(first.getPoDate());
        response.setPoStatus(first.getPoStatus());
        response.setGrandTotal(first.getGrandTotal());
        response.setSupplierId(first.getSupplierId());
        response.setSupplierName(first.getSupplierName());
        response.setTenant(first.getTenant());

        response.setLineItems(
                rows.stream()
                        .map(this::toLineItem)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private PoLineItemForInward toLineItem(IndentsForInwardView v) {
        PoLineItemForInward li = new PoLineItemForInward();
        li.setLineItemCode(v.getLineItemCode());
        li.setIndentId(v.getIndentId());
        li.setProductId(v.getProductId());
        li.setProductName(v.getProductName());
        li.setProductCode(v.getProductCode());
        li.setMeasurementUnit(v.getMeasurementUnit());
        li.setOrderedQuantity(v.getQuantity());
        li.setRemarks(v.getRemarks());
        return li;
    }


    @Transactional(rollbackFor = Exception.class)
    public InwardInventory createInwardnventory(InwardFromPODTO iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        InwardInventory inwardInventory = new InwardInventory();
        editAuthorizationService.validateCreateDate(iiData.getInwardDate());
        List<IndentsForInwardView> pendingItemsForInward = indentsForInwardViewRepository.findPendingLineItemsForPO(IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, iiData.getPoNumber(), tenantService.removePrefixForSuncity(ThreadLocalStorage.getTenantName()));

        if (pendingItemsForInward.isEmpty()) {
            throw new IllegalArgumentException("No pending inward items found for the provided Purchase Order Number - " + iiData.getPoNumber());
        }

        validateInputsFromPO(iiData, pendingItemsForInward);
        setFieldsFromPO(inwardInventory, iiData, pendingItemsForInward);
        updateStockForCreateInwardInventory(inwardInventory);
        inwardInventoryRepo.save(inwardInventory);
        for (InwardOutwardList io : inwardInventory.getInwardOutwardList()) {
            indentReconciliationTaskService.enqueue(io.getLineItemCode());
        }
        return inwardInventory;
    }

    private void setFieldsFromPO(InwardInventory inwardInventory, InwardFromPODTO iiData, List<IndentsForInwardView> pendingItemsForInward) throws Exception {

        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
        inwardInventory.setDate(iiData.getInwardDate());
        inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
        inwardInventory.setVehicleNo(iiData.getVehicleNo());
        inwardInventory.setSupplierSlipNo(iiData.getSupplierSlipNo());
        inwardInventory.setAdditionalInfo(iiData.getAdditionalInfo());
        inwardInventory.setSupplier(supplierRepo.findById(pendingItemsForInward.get(0).getSupplierId()).get());
        inwardInventory.setPurchaseOrderNo(iiData.getPoNumber());
        inwardInventory.setPurchaseOrderDate(pendingItemsForInward.get(0).getPoDate());
        inwardInventory.setChallanDate(iiData.getChallanDate() == null ? null : iiData.getChallanDate());
        inwardInventory.setChallanNo(iiData.getChallanNo() == null ? null : iiData.getChallanNo());
        inwardInventory.setBillDate(iiData.getBillDate() == null ? null : iiData.getBillDate());
        inwardInventory.setBillNo(iiData.getBillNo() == null ? null : iiData.getBillNo());
        inwardInventory.setInwardOutwardList(fetchInwardOutwardListFromPOLine(iiData.getLineItems(), pendingItemsForInward));
        inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
    }

    public Set<InwardOutwardList> fetchInwardOutwardListFromPOLine(List<LineItemForInwardThroughPODTO> lineItems, List<IndentsForInwardView> pendingItemsForInward) {
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (LineItemForInwardThroughPODTO lineItem : lineItems) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            List<IndentsForInwardView> rowsWithLineItemCode = pendingItemsForInward.stream().filter(e -> e.getLineItemCode().equalsIgnoreCase(lineItem.getLineItemCode())).collect(Collectors.toList());

            if (rowsWithLineItemCode.isEmpty()) {
                throw new IllegalArgumentException("No pending inward items found for the line item code - " + lineItem.getLineItemCode());
            }
            IndentsForInwardView row = rowsWithLineItemCode.get(0);
            Product product = productRepo.findById(row.getProductId()).get();
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(lineItem.getQuantityReceived());
            inwardOutwardList.setWarehouse(warehouseRepo.findById(lineItem.getWarehouseId()).get());
            inwardOutwardList.setLineItemCode(row.getLineItemCode());
            inwardOutwardListSet.add(inwardOutwardList);
        }
        return inwardOutwardListSet;
    }

    private void validateInputsFromPO(InwardFromPODTO iiData, List<IndentsForInwardView> pendingItemsForInward) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());


        Set<String> validLineItemCodes = pendingItemsForInward.stream()
                .map(IndentsForInwardView::getLineItemCode)
                .collect(Collectors.toSet());

        for (LineItemForInwardThroughPODTO lineItem : iiData.getLineItems()) {
            if (!warehouseRepo.existsById(lineItem.getWarehouseId()))
                throw new IllegalArgumentException("Warehouse not found with ID - " + lineItem.getWarehouseId());

            if (lineItem.getQuantityReceived() == null || lineItem.getQuantityReceived() <= 0) {
                throw new IllegalArgumentException("Quantity received should be greater than zero for line item code: " + lineItem.getLineItemCode());
            }

            Double poQuantity = pendingItemsForInward.stream()
                    .filter(e -> e.getLineItemCode().equalsIgnoreCase(lineItem.getLineItemCode()))
                    .mapToDouble(IndentsForInwardView::getQuantity)
                    .sum();
            Double inwardQuantity = pendingItemsForInward.stream()
                    .filter(e -> e.getLineItemCode().equalsIgnoreCase(lineItem.getLineItemCode()))
                    .mapToDouble(IndentsForInwardView::getTotalInwardQuantity)
                    .sum();

            double allowedQuantity = poQuantity - inwardQuantity;

            if (lineItem.getQuantityReceived() > allowedQuantity) {
                throw new IllegalArgumentException("Quantity received for line item code " + lineItem.getLineItemCode() +
                        " exceeds the allowed quantity for inward. Allowed quantity: " + allowedQuantity);
            }

            if (!validLineItemCodes.contains(lineItem.getLineItemCode()))
                throw new IllegalArgumentException("Line item code " + lineItem.getLineItemCode() + " is invalid or already fully received.");
        }

        long duplicateProductIdCount = iiData.getLineItems().stream()
                .collect(Collectors.groupingBy(LineItemForInwardThroughPODTO::getLineItemCode, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new IllegalArgumentException("Inventory List should be Unique. Same line item added multiple times.");
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForCreateInwardInventory(InwardInventory inwardInventory) throws Exception {
        Set<InwardOutwardList> productsWithQuantities = inwardInventory.getInwardOutwardList();
        for (InwardOutwardList oiList : productsWithQuantities) {
            Long warehouseId = oiList.getWarehouse().getWarehouseId();
            Long productId = oiList.getProduct().getProductId();
            Double quantity = oiList.getQuantity();
            Double closingStock = stockService.updateStock(productId, warehouseId, quantity, "inward");
            oiList.setClosingStock(closingStock);
        }
    }

    public Pageable modifyPageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        Sort newSort = sort;

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            Sort.Direction direction = order.getDirection();
            if (property.equalsIgnoreCase("date")) {
                if (direction == Sort.Direction.ASC) {
                    newSort = Sort.by(Sort.Order.asc("date"), Sort.Order.desc("inwardid"));
                } else if (direction == Sort.Direction.DESC) {
                    newSort = Sort.by(Sort.Order.desc("date"), Sort.Order.asc("inwardid"));
                }
                pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
                break;
            }
        }

        return pageable;
    }

    /**
     * OLD CODE
     */


    @Transactional(rollbackFor = Exception.class)
    public InwardInventory createInwardnventory(InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        editAuthorizationService.validateCreateDate(iiData.getInwardDate());
        InwardInventory inwardInventory = new InwardInventory();
        validateInputs(iiData);
        setFieldsForInward(inwardInventory, iiData);
        updateStockForCreateInwardInventory(inwardInventory);
        inwardInventoryRepo.save(inwardInventory);
        return inwardInventory;
    }


    @Transactional(rollbackFor = Exception.class)
    public InwardInventory addRejectInwardEntry(ReturnRejectInwardOutwardData rd, Long inwardId) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!inwardInventoryRepo.existsById(inwardId))
            throw new Exception("Inward inventory with ID not found");

        if (rd.getProductWithQuantities().size() == 0)
            throw new Exception("Minimum of one product is required to save data.");

        for (ProductWithQuantity pwq : rd.getProductWithQuantities()) {
            if (pwq.getRemarks() == null)
                throw new Exception(
                        "Remarks is a mandatory field. Please provide remarks for all products before saving data");

            if (pwq.getRemarks().trim().equals(""))
                throw new Exception("Remarks is a mandatory field. Please provide remarks before saving data");
        }

        Long duplicateProductIdCount = rd.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");

        for (ProductWithQuantity productWithQuantity : rd.getProductWithQuantities()) {
            if (productWithQuantity.getQuantity() == null || productWithQuantity.getProductId() == null
                    || !productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Error fetching product details");
            addReturnForInward(inwardId, productWithQuantity.getProductId(), productWithQuantity.getQuantity(),
                    productWithQuantity.getRemarks());
            /*
             * else addRejectForOutward(inwardId, productWithQuantity.getProductId(),
             * productWithQuantity.getQuantity());
             */
        }
        return inwardInventoryRepo.findById(inwardId).get();
    }

    private void addReturnForInward(Long inwardId, Long productId, Double quantity, String remarks) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        InwardInventory ii = inwardInventoryRepo.findById(inwardId).get();
        editAuthorizationService.validateUpdateDates(ii.getDate(), ii.getDate());
        Set<RejectInwardList> rejectInwardList = ii.getRejectInwardList();
        Set<InwardOutwardList> inwardOutwardListSet = ii.getInwardOutwardList();
        for (InwardOutwardList inwardOutwardList : inwardOutwardListSet) {
            Long warehouseId = inwardOutwardList.getWarehouse().getWarehouseId();
            if (inwardOutwardList.getProduct().getProductId().equals(productId)) {
                Double currentQuantity = inwardOutwardList.getQuantity();
                if (quantity > currentQuantity)
                    throw new Exception("Reject quantity cannot be greater than existing quantity for product -" + inwardOutwardList.getProduct().getProductName());

                Double diffInQuantity = currentQuantity - quantity;
                Double closingStock = stockService.updateStock(productId, warehouseId,
                        quantity, "outward");
                rejectInwardList.add(new RejectInwardList(new Date(), inwardOutwardList.getProduct(), currentQuantity,
                        quantity, closingStock, remarks));
                inwardOutwardList.setQuantity(diffInQuantity);
                inwardOutwardList.setClosingStock(closingStock);
            }
        }
        ii.setRejectInwardList(rejectInwardList);
        ii.setInwardOutwardList(inwardOutwardListSet);
        inwardInventoryRepo.save(ii);
    }

    private void setFieldsForInward(InwardInventory inwardInventory, InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
        inwardInventory.setDate(iiData.getInwardDate());
        inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
        inwardInventory.setVehicleNo(iiData.getVehicleNo());
        inwardInventory.setSupplierSlipNo(iiData.getVendorSlipNo());
        inwardInventory.setAdditionalInfo(iiData.getAdditionalInfo());
        inwardInventory.setSupplier(supplierRepo.findById(iiData.getSupplierId()).get());
        //inwardInventory.setWarehouse(warehouseRepo.findById(iiData.getWarehouseId()).get());
        inwardInventory.setInwardOutwardList(fetchInwardOutwardList(iiData.getProductWithQuantities()));
        inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        inwardInventory.setPurchaseOrderNo(null);
        inwardInventory.setPurchaseOrderDate(null);
        inwardInventory.setChallanDate(iiData.getChallanDate() == null ? null : iiData.getChallanDate());
        inwardInventory.setChallanNo(iiData.getChallanNo() == null ? null : iiData.getChallanNo());
        inwardInventory.setBillDate(iiData.getBillDate() == null ? null : iiData.getBillDate());
        inwardInventory.setBillNo(iiData.getBillNo() == null ? null : iiData.getBillNo());
    }

    public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (ProductWithQuantity productWithQuantity : productWithQuantities) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            Product product = productRepo.findById(productWithQuantity.getProductId()).get();
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
            inwardOutwardListSet.add(inwardOutwardList);
            inwardOutwardList.setWarehouse(warehouseRepo.findById(productWithQuantity.getWarehouseId()).get());
        }
        return inwardOutwardListSet;
    }

    public ReturnInwardInventoryData fetchInwardnventory(FilterDataList filterDataList, Pageable pageable)
            throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
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
        return returnInwardInventoryData;
    }

    private void validateInputs(InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());


        for (ProductWithQuantity productWithQuantity : iiData.getProductWithQuantities()) {
            if (!productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Product not found with ID " + productWithQuantity.getProductId());

            if (!warehouseRepo.existsById(productWithQuantity.getWarehouseId()))
                throw new Exception("Warehouse not found");
        }

        if (!supplierRepo.existsById(iiData.getSupplierId()))
            throw new Exception("Supplier not found with ID");

        Long duplicateProductIdCount = iiData.getProductWithQuantities().stream()
                .collect(Collectors.groupingBy(ProductWithQuantity::getProductId, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new Exception("Inventory List should be Unique. Same product added multiple times. Please correct.");

    }


    public List<ProductGroupedDAO> getTotalsForInward(FilterDataList filterDataList) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<InwardInventory> spec = InwardInventorySpecification.getSpecification(filterDataList);
        if (spec != null)
            return fetchGroupingForFilteredData(spec);
        else
            return fetchInwardnventoryGroupBy();
    }

    public List<InwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
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

    public List<ProductGroupedDAO> fetchInwardnventoryGroupBy() throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<ProductGroupedDAO> groupedData = inwardInventoryRepo.findGroupByInfo();
        return groupedData;
    }

    private List<ProductGroupedDAO> fetchGroupingForFilteredData(Specification<InwardInventory> spec) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Map<Pair<String, String>, Double> map = inwardInventoryRepo.findAll(spec).stream()
                .flatMap(i -> i.getInwardOutwardList().stream())
                .collect(Collectors.toMap(l -> Pair.of(l.getProduct().getProductName(), l.getProduct().getMeasurementUnit()),
                        InwardOutwardList::getQuantity,
                        Double::sum));

        List<ProductGroupedDAO> returnData = new ArrayList<>();
        for (Map.Entry<Pair<String, String>, Double> e : map.entrySet()) {
            ProductGroupedDAO rd = new ProductGroupedDAO(
                    e.getKey().getFirst(),
                    e.getKey().getSecond(),
                    e.getValue()
            );
            returnData.add(rd);
        }
        return returnData;
    }

    private List<InwardInventoryExportDAO2> transformDataForExport(List<InwardInventory> iiData) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        List<InwardInventoryExportDAO2> transformedData = new ArrayList<InwardInventoryExportDAO2>();
        for (InwardInventory ii : iiData) {
            for (InwardOutwardList ioList : ii.getInwardOutwardList()) {
                InwardInventoryExportDAO2 ied = new InwardInventoryExportDAO2(ii, ioList);
                transformedData.add(ied);
            }
        }
        return transformedData;
    }

    public InwardInventory findById(long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
        if (inwardInventoryOpt.isPresent())
            return inwardInventoryOpt.get();
        else
            throw new Exception("Inward inventory not found");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteInwardInventoryById(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
        if (!inwardInventoryOpt.isPresent())
            throw new Exception("Inward Inventory with ID not found");
        InwardInventory inwardInventory = inwardInventoryOpt.get();
        editAuthorizationService.validateDeleteDate(inwardInventory.getDate());
        updateStockBeforeDelete(inwardInventory);
        removeOrphans(inwardInventory);
        inwardInventoryRepo.softDeleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    private void removeOrphans(InwardInventory inwardInventory) {
        Set<InwardOutwardList> iolList = inwardInventory.getInwardOutwardList();
        for (InwardOutwardList iol : iolList) {
            iol.setDeleted(true);
            iolRepo.save(iol);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockBeforeDelete(InwardInventory inwardInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        for (InwardOutwardList ioList : inwardInventory.getInwardOutwardList()) {
            Long warehouseId = ioList.getWarehouse().getWarehouseId();
            Double stock = ioList.getQuantity();
            Double currentStock = stockRepo
                    .findStockForProductAndWarehouse(ioList.getProduct().getProductId(), warehouseId).get(0)
                    .getQuantityInHand();
            if (currentStock < stock)
                throw new Exception("Cannot Delete. Stock will go negative if deleted");
            stockService.updateStock(ioList.getProduct().getProductId(), warehouseId, stock, "outward");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception {
        logger.info("In undate inward inventory flow");
        Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
        if (!inwardInventoryOpt.isPresent())
            throw new Exception("Inventory Entry with ID not found");

        return null;
    }
}
