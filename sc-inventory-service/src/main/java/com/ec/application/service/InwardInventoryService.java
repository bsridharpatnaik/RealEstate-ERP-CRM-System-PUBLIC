package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.config.ProjectConstants;
import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.InwardInventoryExportDAO2;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.data.ProductWithQuantity;
import com.ec.application.data.ReturnInwardInventoryData;
import com.ec.application.data.ReturnRejectInwardOutwardData;
import com.ec.application.data.UserReturnData;
import com.ec.application.model.APICallTypeForAuthorization;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.Product;
import com.ec.application.model.RejectInwardList;
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

    Logger log = LoggerFactory.getLogger(InwardInventoryService.class);

    @Transactional(rollbackFor = Exception.class)
    public InwardInventory createInwardnventory(InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        InwardInventory inwardInventory = new InwardInventory();
        validateInputs(iiData);
        exitIfNotAuthorized(inwardInventory, iiData, APICallTypeForAuthorization.Create);
        setFields(inwardInventory, iiData);
        updateStockForCreateInwardInventory(inwardInventory);
        return inwardInventoryRepo.save(inwardInventory);
    }

    @Transactional(rollbackFor = Exception.class)
    private void updateStockForCreateInwardInventory(InwardInventory inwardInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> productsWithQuantities = inwardInventory.getInwardOutwardList();
        String warehouseName = inwardInventory.getWarehouse().getWarehouseName();

        for (InwardOutwardList oiList : productsWithQuantities) {
            Long productId = oiList.getProduct().getProductId();
            Double quantity = oiList.getQuantity();
            Double closingStock = stockService.updateStock(productId, warehouseName, quantity, "inward");
            oiList.setClosingStock(closingStock);
        }
    }

    public void backFillClosingStock() {
        asyncService.run(() ->
        {
            try {
                asyncServiceInventory.backFillClosingStock(ThreadLocalStorage.getTenantName());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
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
        exitIfNotAuthorized(ii, null, APICallTypeForAuthorization.Reject);
        Set<RejectInwardList> rejectInwardList = ii.getRejectInwardList();
        Set<InwardOutwardList> inwardOutwardListSet = ii.getInwardOutwardList();
        for (InwardOutwardList inwardOutwardList : inwardOutwardListSet) {
            if (inwardOutwardList.getProduct().getProductId().equals(productId)) {
                Double currentQuantity = inwardOutwardList.getQuantity();
                if (quantity > currentQuantity)
                    throw new Exception(
                            "Reject quantity cannot be greater than existing quantity for product -"
                                    + inwardOutwardList.getProduct().getProductName());

                Double diffInQuantity = currentQuantity - quantity;
                Double closingStock = stockService.updateStock(productId, ii.getWarehouse().getWarehouseName(),
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

    private void setFields(InwardInventory inwardInventory, InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
        // inwardInventory.setInvoiceReceived(false);
        inwardInventory.setDate(iiData.getDate());
        inwardInventory.setOurSlipNo(iiData.getOurSlipNo());
        inwardInventory.setVehicleNo(iiData.getVehicleNo());
        inwardInventory.setVendorSlipNo(iiData.getVendorSlipNo());
        inwardInventory.setAdditionalInfo(iiData.getAdditionalInfo());
        inwardInventory.setSupplier(supplierRepo.findById(iiData.getSupplierId()).get());
        inwardInventory.setWarehouse(warehouseRepo.findById(iiData.getWarehouseId()).get());
        inwardInventory.setInwardOutwardList(fetchInwardOutwardList(iiData.getProductWithQuantities(),
                warehouseRepo.findById(iiData.getWarehouseId()).get()));
        inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        inwardInventory.setPurchaseOrder(iiData.getPurchaseOrder());
        inwardInventory.setPurchaseOrderdate(iiData.getPurchaseOrderDate());
        inwardInventory.setChallanDate(iiData.getChallanDate() == null ? null : iiData.getChallanDate());
        inwardInventory.setChallanNo(iiData.getChallanNo() == null ? null : iiData.getChallanNo());
        inwardInventory.setBillDate(iiData.getBillDate() == null ? null : iiData.getBillDate());
        inwardInventory.setBillNo(iiData.getBillNo() == null ? null : iiData.getBillNo());
    }

    public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities,
                                                         Warehouse warehouse) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (ProductWithQuantity productWithQuantity : productWithQuantities) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            Product product = productRepo.findById(productWithQuantity.getProductId()).get();
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
            inwardOutwardListSet.add(inwardOutwardList);
        }
        return inwardOutwardListSet;
    }

    private void validateInputs(InwardInventoryData iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (iiData.getPurchaseOrderDate() == null)
            throw new Exception("Purchase Order Date is a mandatory field");

        for (ProductWithQuantity productWithQuantity : iiData.getProductWithQuantities()) {
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
            throws ParseException {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        backFillClosingStock();
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

    private List<ProductGroupedDAO> fetchGroupingForFilteredData(Specification<InwardInventory> spec,
                                                                 Class<InwardInventory> class1) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Map<Pair<String, String>, Double> map = inwardInventoryRepo.findAll(spec).stream()
                .flatMap(i -> i.getInwardOutwardList().stream())
                .collect(Collectors.toMap(l -> Pair.of(l.getProduct().getProductName(), l.getProduct().getMeasurementUnit()),
                        InwardOutwardList::getQuantity,
                        Double::sum));

        List<ProductGroupedDAO> returnData = new ArrayList<>();
        for(Map.Entry< Pair<String, String>, Double> e: map.entrySet()){
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
        exitIfNotAuthorized(inwardInventory, null, APICallTypeForAuthorization.Delete);
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

    @Transactional
    private void updateStockBeforeDelete(InwardInventory inwardInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        String warehouseName = inwardInventory.getWarehouse().getWarehouseName();
        for (InwardOutwardList ioList : inwardInventory.getInwardOutwardList()) {
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
    public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception {
        logger.info("In undate inward inventory flow");
        Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
        if (!inwardInventoryOpt.isPresent())
            throw new Exception("Inventory Entry with ID not found");
        validateInputs(iiData);
        InwardInventory inwardInventory = inwardInventoryOpt.get();
        InwardInventory oldInwardInventory = (InwardInventory) inwardInventory.clone();
        exitIfNotAuthorized(inwardInventory, iiData, APICallTypeForAuthorization.Update);
        setFields(inwardInventory, iiData);
        modifyStockBeforeUpdate(oldInwardInventory, inwardInventory);
        removeOrphans(oldInwardInventory);
        return inwardInventoryRepo.save(inwardInventory);

    }

    @Transactional
    private void exitIfNotAuthorized(InwardInventory inwardInventory, InwardInventoryData iiData,
                                     APICallTypeForAuthorization action) throws Exception {

        UserReturnData currentUserData = userDetailService.getCurrentUser();

        if (action.equals(APICallTypeForAuthorization.Update)) {
            if (currentUserData.getRoles().contains("admin")
                    || currentUserData.getRoles().contains("inventory-manager")) {
                if (ReusableMethods.daysBetweenTwoDates(inwardInventory.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysAdmin)
                    throw new Exception("Cannot modify record that is created greater than 30 days ago.");

            } else if (currentUserData.getRoles().contains("inventory-executive")) {
                if (ReusableMethods.daysBetweenTwoDates(inwardInventory.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysExecutive)
                    throw new Exception("Cannot modify record that is created greater than "
                            + ProjectConstants.editAllowedDaysExecutive + " days ago.");
            } else {
                throw new Exception("No User role found for user!. Please contact administration to get roles added");
            }
            if (!iiData.getDate().equals(inwardInventory.getDate()))
                throw new Exception("Date should not be modified while updating inward inventory record");

            if (!iiData.getWarehouseId().equals(inwardInventory.getWarehouse().getWarehouseId()))
                throw new Exception("Warehouse should not be modified while updating inward inventory record");

            List<Long> productsInPayload = iiData.getProductWithQuantities().stream()
                    .map(ProductWithQuantity::getProductId).collect(Collectors.toList());

            List<Long> productsInExistingRecord = inwardInventory.getInwardOutwardList().stream()
                    .map(InwardOutwardList::getProduct).map(Product::getProductId).collect(Collectors.toList());
            if (!(productsInPayload.containsAll(productsInExistingRecord)
                    && productsInPayload.size() == productsInExistingRecord.size()))
                throw new Exception("Inventory List should not be modified while updating an inward inventory record");
        }
        if (action.equals(APICallTypeForAuthorization.Create)) {
            if (currentUserData.getRoles().contains("admin")
                    || currentUserData.getRoles().contains("inventory-manager")) {
                if (ReusableMethods.daysBetweenTwoDates(iiData.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysAdmin)
                    throw new Exception("Cannot create inward inventory with date more than "
                            + ProjectConstants.editAllowedDaysAdmin + " Days in past. ");
            } else if (currentUserData.getRoles().contains("inventory-executive")) {
                if (ReusableMethods.daysBetweenTwoDates(iiData.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysExecutive)
                    throw new Exception("Cannot create inward inventory with date more than "
                            + ProjectConstants.editAllowedDaysExecutive + " Days in past. ");
            } else {
                throw new Exception("No User role found for user!. Please contact administration to get roles added");
            }
        }
        if (action.equals(APICallTypeForAuthorization.Delete) || action.equals(APICallTypeForAuthorization.Reject)) {
            if (currentUserData.getRoles().contains("admin")
                    || currentUserData.getRoles().contains("inventory-manager")) {
                if (ReusableMethods.daysBetweenTwoDates(inwardInventory.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysAdmin)
                    throw new Exception("Cannot DELETE inward inventory created more than "
                            + ProjectConstants.editAllowedDaysAdmin + " Days ago. ");
            } else if (currentUserData.getRoles().contains("inventory-executive")) {
                if (ReusableMethods.daysBetweenTwoDates(inwardInventory.getDate(),
                        new Date()) > ProjectConstants.editAllowedDaysExecutive)
                    throw new Exception("Cannot DELETE inward inventory created more than "
                            + ProjectConstants.editAllowedDaysExecutive + " Days ago. ");
            } else {
                throw new Exception("No User role found for user!. Please contact administration to get roles added");
            }
        }
    }

    @Transactional
    private void modifyStockBeforeUpdate(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!oldInwardInventory.getWarehouse().getWarehouseId().equals(inwardInventory.getWarehouse().getWarehouseId()))
            updateWhenWarehouseChanged(oldInwardInventory, inwardInventory);
        else
            updateWhenWarehouseSame(oldInwardInventory, inwardInventory);
    }

    @Transactional
    private void updateWhenWarehouseSame(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
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
                                            InwardInventory inwardInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        logger.info("Updating stock for productid common in both old and new");
        Set<InwardOutwardList> oldIOListSet = oldInwardInventory.getInwardOutwardList();
        Set<InwardOutwardList> newIOListSet = inwardInventory.getInwardOutwardList();
        for (Long id : commonInBoth) {

            Double oldQuantity = findQuantityForProductInIOList(id, oldIOListSet);
            Double newQuantity = findQuantityForProductInIOList(id, newIOListSet);
            Double quantityForUpdate = newQuantity - oldQuantity;
            logger.info("Difference in quantity -" + quantityForUpdate);
            for (InwardOutwardList ioList : newIOListSet) {

                if (id.equals(ioList.getProduct().getProductId())) {
                    Double closingStock = stockService.updateStock(id,
                            inwardInventory.getWarehouse().getWarehouseName(), quantityForUpdate, "inward");
                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            inwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
                    ioList.setClosingStock(closingStock);
                }
            }
            inwardInventory.setInwardOutwardList(newIOListSet);
        }

    }

    @Transactional
    private Double findQuantityForProductInIOList(Long productId, Set<InwardOutwardList> ioListSet) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        for (InwardOutwardList ioList : ioListSet) {
            if (productId.equals(ioList.getProduct().getProductId())) {
                Double oldQuantity = ioList.getQuantity();
                return oldQuantity;
            }
        }
        return null;
    }

    @Transactional
    private void updateStockForOnlyInNew(Set<Long> onlyInNew, InwardInventory inwardInventory) throws Exception {
        logger.info("Processing Update Stock for productids present only in new");
        for (Long id : onlyInNew) {
            Set<InwardOutwardList> ioListSet = inwardInventory.getInwardOutwardList();

            for (InwardOutwardList ioList : ioListSet) {
                if (id.equals(ioList.getProduct().getProductId())) {
                    Double quantity = ioList.getQuantity();
                    Double closingStock = stockService.updateStock(id,
                            inwardInventory.getWarehouse().getWarehouseName(), quantity, "inward");
                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            inwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
                    ioList.setClosingStock(closingStock);
                }
            }
            inwardInventory.setInwardOutwardList(ioListSet);
        }

    }

    @Transactional
    private void updateStockForOnlyInOld(Set<Long> onlyInOld, InwardInventory oldInwardInventory) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        // Delete stock received as part of old inventory
        for (Long id : onlyInOld) {
            Set<InwardOutwardList> ioListSet = oldInwardInventory.getInwardOutwardList();
            for (InwardOutwardList ioList : ioListSet) {
                if (id.equals(ioList.getProduct().getProductId())) {
                    Double quantity = ioList.getQuantity();
                    Double closingStock = stockService.updateStock(id,
                            oldInwardInventory.getWarehouse().getWarehouseName(), quantity, "outward");

                    inventoryNotificationService.pushQuantityEditedNotification(ioList.getProduct(),
                            oldInwardInventory.getWarehouse().getWarehouseName(), "inward", closingStock);
                }
            }
        }
    }

    @Transactional
    private void updateWhenWarehouseChanged(InwardInventory oldInwardInventory, InwardInventory inwardInventory)
            throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
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
                                                              Warehouse warehouse) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        for (InwardOutwardList oiList : ioListset) {
            Double closingStock = stockService.updateStock(oiList.getProduct().getProductId(),
                    warehouse.getWarehouseName(), oiList.getQuantity(), type);
            inventoryNotificationService.pushQuantityEditedNotification(oiList.getProduct(),
                    warehouse.getWarehouseName(), "inward", closingStock);
            oiList.setClosingStock(closingStock);
        }
        return ioListset;
    }
}
