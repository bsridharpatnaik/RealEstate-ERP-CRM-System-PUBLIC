package com.ec.application.service;

import static java.util.stream.Collectors.counting;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.InwardActionType;
import com.ec.application.data.*;
import com.ec.application.indentpo.IndentInventoryAsyncUpdater;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.*;

import java.time.LocalDate;
import java.time.ZoneId;
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
import com.ec.application.ReusableClasses.ActivityLogDescription;
import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.InwardInventorySpecification;
import org.springframework.context.ApplicationEventPublisher;
import com.ec.application.indentpo.InwardSyncEvent;

@Service
@Transactional
public class InwardInventoryService {

    Logger logger = LoggerFactory.getLogger(InwardInventoryService.class);

    @Autowired
    InwardInventoryRepo inwardInventoryRepo;

    @Autowired
    InwardOutwardListRepo iolRepo;

    @Autowired
    ProductRepo productRepo;

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
    IndentsForInwardViewRepository indentsForInwardViewRepository;

    @Autowired
    TenantService tenantService;

    @Autowired
    EditAuthorizationService editAuthorizationService;

    @Autowired
    IndentInventoryAsyncUpdater indentInventoryAsyncUpdater;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    PurchaseOrderShortClosedViewRepo purchaseOrderShortClosedViewRepo;

    @Autowired
    InventoryNotificationService inventoryNotificationService;

    @Autowired
    MrnService mrnService;

    @Autowired
    SystemContactRepo systemContactRepo;

    @Autowired
    InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    UserDetailsService userDetailsService;

    Logger log = LoggerFactory.getLogger(InwardInventoryService.class);

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }

    public List<PoDropdownItem> getPendingPoDropdown() {
        String tenant = ThreadLocalStorage.getTenantName();
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
        String tenant = ThreadLocalStorage.getTenantName();
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
        li.setSpecification(v.getSpecification());
        double tolerancePct    = v.getTolerancePercent() != null ? v.getTolerancePercent() : 0.0;
        double indentQty       = v.getQuantity() != null ? v.getQuantity() : 0.0;
        double alreadyInwarded = v.getTotalInwardQuantity() != null ? v.getTotalInwardQuantity() : 0.0;
        double pendingQty      = Math.max(indentQty - alreadyInwarded, 0.0);
        // maxAllowed = pending qty + tolerance buffer (based on original ordered qty)
        double maxAllowed      = pendingQty + (indentQty * tolerancePct / 100.0);
        li.setTolerancePercent(tolerancePct);
        li.setTotalInwardQuantity(alreadyInwarded);
        li.setPendingQuantity(pendingQty);
        li.setMaxAllowedQuantity(maxAllowed);
        productRepo.findById(v.getProductId()).ifPresent(p -> {
            li.setIsExpirable(p.requiresExpiry());
            li.setBatchMode(p.getBatchMode());
        });
        return li;
    }


    @Transactional(rollbackFor = Exception.class)
    public InwardInventory createInwardnventory(InwardFromPODTO iiData) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        InwardInventory inwardInventory = new InwardInventory();
        editAuthorizationService.validateCreateDate(iiData.getInwardDate());
        List<IndentsForInwardView> pendingItemsForInward = indentsForInwardViewRepository.findPendingLineItemsForPO(IndentLineItemStatusConstants.INWARD_ELIGIBLE_STATUSES, iiData.getPoNumber(), ThreadLocalStorage.getTenantName());

        if (pendingItemsForInward.isEmpty()) {
            throw new IllegalArgumentException("No pending inward items found for the provided Purchase Order Number - " + iiData.getPoNumber());
        }

        Date poDate = pendingItemsForInward.get(0).getPoDate();
        if (poDate != null && iiData.getInwardDate().before(poDate)) {
            throw new IllegalArgumentException("Inward date cannot be earlier than the PO date (" + new java.text.SimpleDateFormat("dd-MM-yyyy").format(poDate) + ").");
        }

        validateInputsFromPO(iiData, pendingItemsForInward);
        setFieldsFromPO(inwardInventory, iiData, pendingItemsForInward);
        updateStockForCreateInwardInventory(inwardInventory);
        inwardInventoryRepo.save(inwardInventory);
        Map<Long, List<InwardBatchSplit>> poSplitsMap = new HashMap<>();
        for (LineItemForInwardThroughPODTO li : iiData.getLineItems()) {
            if (li.getBatchSplits() != null && !li.getBatchSplits().isEmpty()) {
                pendingItemsForInward.stream()
                        .filter(r -> r.getLineItemCode().equalsIgnoreCase(li.getLineItemCode()))
                        .findFirst()
                        .ifPresent(r -> poSplitsMap.put(r.getProductId(), li.getBatchSplits()));
            }
        }
        createBatchesForInward(inwardInventory, poSplitsMap);
        List<IndentInwardDeltaDTO> deltas = new ArrayList<>();

        for (InwardOutwardList io : inwardInventory.getInwardOutwardList()) {
            deltas.add(new IndentInwardDeltaDTO(io.getLineItemCode(), io.getQuantity()));
        }

        IndentInwardSyncDTO syncDTO = new IndentInwardSyncDTO(inwardInventory.getDate(), ThreadLocalStorage.getTenantName(), inwardInventory.getInwardId(), InwardActionType.CREATE, inwardInventory.getPurchaseOrderNo(), deltas);
        applicationEventPublisher.publishEvent(new InwardSyncEvent(this, syncDTO, "create"));
        String createPoUser = resolveCurrentUser();
        List<Map<String, Object>> createPoItems = inwardInventory.getInwardOutwardList().stream()
                .map(io -> ActivityLogDescription.item(io.getProduct().getProductName(), io.getQuantity()))
                .collect(Collectors.toList());
        activityLogService.record("CREATED", "INWARD", String.valueOf(inwardInventory.getInwardId()),
                ActivityLogDescription.withItemsAndType("Inward " + inwardInventory.getInwardId()
                        + " created from PO " + iiData.getPoNumber() + " by " + createPoUser, "From PO", createPoItems),
                createPoUser);
        return inwardInventory;
    }

    @Transactional(rollbackFor = Exception.class)
    public InwardInventory updateInwardInventory(Long inwardId, InwardInventoryUpdateData data) throws Exception {

        log.info("Invoked updateInwardInventory for id={}", inwardId);

        InwardInventory inward = inwardInventoryRepo.findById(inwardId).orElseThrow(() -> new Exception("Inward Inventory not found with id=" + inwardId));
        editAuthorizationService.validateUpdateDates(inward.getDate(), inward.getDate());
        Set<Long> existingProductIds = inward.getInwardOutwardList().stream().map(io -> io.getProduct().getProductId()).collect(Collectors.toSet());
        Set<Long> payloadProductIds = data.getProductWithQuantities().stream().map(ProductAndQuantity::getProductId).collect(Collectors.toSet());

        if (!existingProductIds.equals(payloadProductIds)) {
            throw new Exception("Product list mismatch during inward update. " + "Adding or removing products is not allowed.");
        }

        if (inward.getCreatedFromPO() && !inward.getSupplier().getContactId().equals(data.getSupplierId())) {
            throw new Exception("Supplier change not allowed for inward created from PO.");
        }

        Map<String, Double> oldQuantityMap = new HashMap<>();
        for (InwardOutwardList io : inward.getInwardOutwardList()) {
            oldQuantityMap.put(io.getLineItemCode(), io.getQuantity());
        }

        if (data.getInwardDate() != null) {
            if (Boolean.TRUE.equals(inward.getCreatedFromPO()) && inward.getPurchaseOrderDate() != null
                    && data.getInwardDate().before(inward.getPurchaseOrderDate())) {
                throw new IllegalArgumentException("Inward date cannot be earlier than the PO date (" + new java.text.SimpleDateFormat("dd-MM-yyyy").format(inward.getPurchaseOrderDate()) + ").");
            }
            inward.setDate(data.getInwardDate());
        }

        boolean noChallanUpd = data.getChallanNo() == null || data.getChallanNo().trim().isEmpty();
        boolean noBillUpd = data.getBillNo() == null || data.getBillNo().trim().isEmpty();
        if (noChallanUpd && noBillUpd) {
            if (data.getNoChallanBillReason() == null || data.getNoChallanBillReason().trim().isEmpty())
                throw new Exception("Please provide a reason since both Challan No. and Bill No. are missing.");
        }

        inward.setSupplier(supplierRepo.findById(data.getSupplierId()).get());
        inward.setVehicleNo(data.getVehicleNo());
        inward.setSupplierSlipNo(data.getSupplierSlipNo());
        //inward.setOurSlipNo(data.getOurSlipNo());  // MRN should be auto calculated
        inward.setAdditionalInfo(data.getAdditionalInfo());
        inward.setInvoiceReceived(data.getInvoiceReceived());
        inward.setChallanNo(data.getChallanNo());
        inward.setBillNo(data.getBillNo());
        inward.setChallanDate(data.getChallanDate());
        inward.setBillDate(data.getBillDate());
        inward.setNoChallanBillReason(data.getNoChallanBillReason());

        if (data.getFileInformations() != null) {
            inward.setFileInformations(ReusableMethods.convertFilesListToSet(data.getFileInformations()));
        }

        // -------------------------------------------------
        // Build lookup map from request
        // -------------------------------------------------
        Map<Long, Double> qtyByProductId = new HashMap<Long, Double>();

        for (ProductAndQuantity pq : data.getProductWithQuantities()) {
            qtyByProductId.put(pq.getProductId(), pq.getQuantity());
        }


        // ✅ VALIDATE INDENT QUANTITY LIMIT FOR PO-LINKED INWARDS
        if (Boolean.TRUE.equals(inward.getCreatedFromPO())) {
            for (InwardOutwardList io : inward.getInwardOutwardList()) {
                String lineItemCode = io.getLineItemCode();
                Long productId = io.getProduct().getProductId();
                Double newQty = qtyByProductId.get(productId);
                Double oldQty = oldQuantityMap.get(lineItemCode);

                // Only validate if quantity is actually increasing
                if (newQty != null && oldQty != null && newQty > oldQty) {
                    List<IndentsForInwardView> lineItemDetails =
                            indentsForInwardViewRepository.getLineItemDetails(
                                    lineItemCode, ThreadLocalStorage.getTenantName()
                            );

                    if (!lineItemDetails.isEmpty()) {
                        IndentsForInwardView view = lineItemDetails.get(0);
                        double indentQty       = view.getQuantity()          != null ? view.getQuantity()          : 0.0;
                        double tolerancePct    = view.getTolerancePercent()  != null ? view.getTolerancePercent()  : 0.0;
                        // Option B: tolerance budget is per-indent — indentQty × (1 + tolerance%)
                        double maxAllowed      = indentQty * (1 + tolerancePct / 100.0);
                        double alreadyInwarded = view.getTotalInwardQuantity(); // includes THIS inward
                        // Subtract this inward's old qty because the view already counts it,
                        // then add the new qty to check the resulting total
                        double allowedQty = maxAllowed - alreadyInwarded + oldQty;

                        if (newQty > allowedQty) {
                            throw new IllegalArgumentException(
                                    "Quantity for '" + view.getProductName() + "' (Line: " + lineItemCode + ")" +
                                    " exceeds allowed limit. Indent Qty: " + indentQty +
                                    ", Tolerance: " + tolerancePct + "%" +
                                    ", Max Allowed: " + allowedQty +
                                    ", Requested: " + newQty
                            );
                        }
                    }
                }
            }
        }

        // -------------------------------------------------
        // 5️⃣ Update quantities + stock adjustment
        // -------------------------------------------------
        for (InwardOutwardList io : inward.getInwardOutwardList()) {
            Long productId = io.getProduct().getProductId();
            if (!qtyByProductId.containsKey(productId)) {
                throw new Exception("Product removal not allowed during inward update. ProductId=" + productId);
            }

            Double oldQty = io.getQuantity();
            Double newQty = qtyByProductId.get(productId);

            if (newQty == null || newQty <= 0) {
                throw new Exception("Invalid quantity for productId=" + productId);
            }

            double delta = newQty - oldQty;

            // ---------------------------------------------
            // 5️⃣.1 Stock reconciliation
            // ---------------------------------------------
            if (delta != 0.0) {

                String direction;
                double absQty;

                if (delta > 0) {
                    direction = "inward";
                    absQty = delta;
                } else {
                    direction = "outward";
                    absQty = Math.abs(delta);
                }
                Double closingStock = stockService.updateStock(productId, io.getWarehouse().getWarehouseId(), absQty, direction);
                io.setClosingStock(closingStock);
                inventoryNotificationService.pushQuantityEditedNotification(io.getProduct(), io.getWarehouse().getWarehouseName(), direction, closingStock);
            }

            // ---------------------------------------------
            // Update quantity
            // ---------------------------------------------
            io.setQuantity(newQty);
        }
        inwardInventoryRepo.save(inward);
        reconcileBatchesForEditedInward(inward, data.getProductWithQuantities());

        // -------------------------------------------------
        // Trigger async indent / PO reconciliation
        // -------------------------------------------------
        if (Boolean.TRUE.equals(inward.getCreatedFromPO())) {

            List<IndentInwardDeltaDTO> deltas = new ArrayList<>();
            for (InwardOutwardList io : inward.getInwardOutwardList()) {
                Double oldQty = oldQuantityMap.get(io.getLineItemCode());
                Double newQty = io.getQuantity();
                // Only send if quantity actually changed
                if (oldQty == null || Double.compare(oldQty, newQty) != 0) {
                    deltas.add(new IndentInwardDeltaDTO(io.getLineItemCode(), newQty));
                }
            }

            if (!deltas.isEmpty()) {
                IndentInwardSyncDTO syncDTO = new IndentInwardSyncDTO(inward.getDate(), ThreadLocalStorage.getTenantName(), inward.getInwardId(), InwardActionType.UPDATE, inward.getPurchaseOrderNo(), deltas);
                applicationEventPublisher.publishEvent(new InwardSyncEvent(this, syncDTO, "update"));
            }
        }
        String updateUser = resolveCurrentUser();
        List<Map<String, Object>> changedItems = ActivityLogDescription.list();
        for (InwardOutwardList io : inward.getInwardOutwardList()) {
            Double oldQty = oldQuantityMap.get(io.getLineItemCode());
            if (oldQty != null && Double.compare(oldQty, io.getQuantity()) != 0) {
                String productName = io.getProduct() != null ? io.getProduct().getProductName() : io.getLineItemCode();
                changedItems.add(ActivityLogDescription.itemChanged(productName, oldQty, io.getQuantity()));
            }
        }
        if (!changedItems.isEmpty()) {
            activityLogService.record("UPDATED", "INWARD", String.valueOf(inward.getInwardId()),
                    ActivityLogDescription.withItems("Inward " + inward.getInwardId() + " updated by " + updateUser, changedItems),
                    updateUser);
        } else {
            activityLogService.record("UPDATED", "INWARD", String.valueOf(inward.getInwardId()),
                    ActivityLogDescription.of("Inward " + inward.getInwardId() + " updated by " + updateUser),
                    updateUser);
        }
        return inward;
    }


    private void setFieldsFromPO(InwardInventory inwardInventory, InwardFromPODTO iiData, List<IndentsForInwardView> pendingItemsForInward) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
        inwardInventory.setDate(iiData.getInwardDate());
        inwardInventory.setOurSlipNo(mrnService.getNextMrn().toString());
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
        inwardInventory.setNoChallanBillReason(iiData.getNoChallanBillReason());
        inwardInventory.setInwardOutwardList(fetchInwardOutwardListFromPOLine(iiData.getLineItems(), pendingItemsForInward));
        inwardInventory.setFileInformations(ReusableMethods.convertFilesListToSet(iiData.getFileInformations()));
        inwardInventory.setCreatedFromPO(true);
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
            boolean hasSplits = lineItem.getBatchSplits() != null && !lineItem.getBatchSplits().isEmpty();
            if (product.isBatchTracked()) {
                if (!hasSplits && product.requiresExpiry() && lineItem.getExpiryDate() == null)
                    throw new IllegalArgumentException(
                            "Expiry date is required for product: '" + product.getProductName() + "'");
                if (hasSplits) validateBatchSplits(lineItem.getBatchSplits(), lineItem.getQuantityReceived(), product.getProductName(), product.requiresExpiry());
            }
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(lineItem.getQuantityReceived());
            inwardOutwardList.setWarehouse(warehouseRepo.findById(lineItem.getWarehouseId()).get());
            inwardOutwardList.setLineItemCode(row.getLineItemCode());
            inwardOutwardList.setIndentRemarks(row.getRemarks());
            inwardOutwardList.setIndentSpecification(row.getSpecification());
            inwardOutwardList.setBrand(hasSplits ? lineItem.getBatchSplits().get(0).getBrand() : lineItem.getBrand());
            inwardOutwardList.setExpiryDate(hasSplits ? lineItem.getBatchSplits().get(0).getExpiryDate() : lineItem.getExpiryDate());
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

            IndentsForInwardView matchedView = pendingItemsForInward.stream()
                    .filter(e -> e.getLineItemCode().equalsIgnoreCase(lineItem.getLineItemCode()))
                    .findFirst()
                    .orElse(null);

            double indentQty     = matchedView != null && matchedView.getQuantity()          != null ? matchedView.getQuantity()          : 0.0;
            double tolerancePct  = matchedView != null && matchedView.getTolerancePercent()  != null ? matchedView.getTolerancePercent()  : 0.0;
            double alreadyInwarded = pendingItemsForInward.stream()
                    .filter(e -> e.getLineItemCode().equalsIgnoreCase(lineItem.getLineItemCode()))
                    .mapToDouble(IndentsForInwardView::getTotalInwardQuantity)
                    .sum();

            double maxAllowed    = indentQty * (1 + tolerancePct / 100.0);
            double allowedQuantity = maxAllowed - alreadyInwarded;

            if (lineItem.getQuantityReceived() > allowedQuantity) {
                String productName = matchedView != null ? matchedView.getProductName() : lineItem.getLineItemCode();
                throw new IllegalArgumentException(
                        "Quantity received for '" + productName + "' (Line: " + lineItem.getLineItemCode() + ")" +
                        " exceeds allowed limit." +
                        " Indent Qty: " + indentQty +
                        (tolerancePct > 0 ? ", Tolerance: " + tolerancePct + "%, Max Allowed: " + allowedQuantity : ", Max Allowed: " + allowedQuantity) +
                        ", Requested: " + lineItem.getQuantityReceived()
                );
            }

            if (!validLineItemCodes.contains(lineItem.getLineItemCode()))
                throw new IllegalArgumentException("Line item code " + lineItem.getLineItemCode() + " is invalid or already fully received.");
        }

        long duplicateProductIdCount = iiData.getLineItems().stream()
                .collect(Collectors.groupingBy(LineItemForInwardThroughPODTO::getLineItemCode, counting())).entrySet().stream()
                .filter(e -> e.getValue() > 1).count();

        if (duplicateProductIdCount > 0)
            throw new IllegalArgumentException("Inventory List should be Unique. Same line item added multiple times.");

        boolean noChallanPO = iiData.getChallanNo() == null || iiData.getChallanNo().trim().isEmpty();
        boolean noBillPO = iiData.getBillNo() == null || iiData.getBillNo().trim().isEmpty();
        if (noChallanPO && noBillPO) {
            if (iiData.getNoChallanBillReason() == null || iiData.getNoChallanBillReason().trim().isEmpty())
                throw new IllegalArgumentException("Please provide a reason since both Challan No. and Bill No. are missing.");
        }
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
        Map<Long, List<InwardBatchSplit>> directSplitsMap = new HashMap<>();
        for (ProductWithQuantity pwq : iiData.getProductWithQuantities()) {
            if (pwq.getBatchSplits() != null && !pwq.getBatchSplits().isEmpty()) {
                directSplitsMap.put(pwq.getProductId(), pwq.getBatchSplits());
            }
        }
        createBatchesForInward(inwardInventory, directSplitsMap);
        String directCreateUser = resolveCurrentUser();
        List<Map<String, Object>> directItems = inwardInventory.getInwardOutwardList().stream()
                .map(io -> ActivityLogDescription.item(
                        io.getProduct() != null ? io.getProduct().getProductName() : io.getLineItemCode(),
                        io.getQuantity()))
                .collect(Collectors.toList());
        String directInwardType = Boolean.TRUE.equals(iiData.getIsSampleInward()) ? "Sample Inward" : "Direct Inward";
        activityLogService.record("CREATED", "INWARD", String.valueOf(inwardInventory.getInwardId()),
                ActivityLogDescription.withItemsAndType("Inward " + inwardInventory.getInwardId()
                        + " created by " + directCreateUser, directInwardType, directItems),
                directCreateUser);
        return inwardInventory;
    }


    @Transactional(rollbackFor = Exception.class)
    public InwardInventory addRejectInwardEntry(ReturnRejectInwardOutwardData rd, Long inwardId) throws Exception {
        log.info("Invoked - addRejectInwardEntry");

        // ---------- Basic validations ----------
        if (!inwardInventoryRepo.existsById(inwardId)) {
            throw new Exception("Inward inventory with ID not found");
        }

        if (rd.getProductWithQuantities() == null || rd.getProductWithQuantities().isEmpty()) {
            throw new Exception("Minimum of one product is required to save data.");
        }

        // ---------- Duplicate product check in request ----------
        boolean hasDuplicates = rd.getProductWithQuantities().stream()
                .map(ProductWithQuantity::getProductId)
                .distinct()
                .count() != rd.getProductWithQuantities().size();

        if (hasDuplicates) {
            throw new Exception("Inventory List should be unique. Same product added multiple times. Please correct.");
        }

        // ---------- Validate & process ----------
        Set<Long> processedProductIds = new HashSet<>();
        for (ProductWithQuantity pwq : rd.getProductWithQuantities()) {

            if (pwq.getRemarks() == null || pwq.getRemarks().trim().isEmpty()) {
                throw new Exception("Remarks is a mandatory field. Please provide remarks for all products before saving data");
            }

            if (!productRepo.existsById(pwq.getProductId())) {
                throw new Exception("Product not found with ID - " + pwq.getProductId());
            }

            // ---------- Process only once per product ----------
            if (processedProductIds.add(pwq.getProductId())) {
                addReturnForInward(inwardId, pwq.getProductId(), pwq.getQuantity(), pwq.getRemarks(), pwq.getOverrideBatches());
            }
        }

        String rejectUser = resolveCurrentUser();
        List<Map<String, Object>> rejectItems = rd.getProductWithQuantities().stream()
                .map(pwq -> {
                    String name = productRepo.findById(pwq.getProductId())
                            .map(p -> p.getProductName()).orElse("ID:" + pwq.getProductId());
                    return ActivityLogDescription.item(name, pwq.getQuantity());
                })
                .collect(Collectors.toList());
        activityLogService.record("REJECTED", "INWARD", String.valueOf(inwardId),
                ActivityLogDescription.withItems("Inward " + inwardId + " rejected by " + rejectUser, rejectItems),
                rejectUser);

        return inwardInventoryRepo.findById(inwardId).get();
    }

    @Transactional(rollbackFor = Exception.class)
    private void addReturnForInward(
            Long inwardId,
            Long productId,
            Double quantity,
            String remarks,
            List<com.ec.application.data.BatchOverrideEntry> overrideBatches
    ) throws Exception {

        log.info("Invoked addReturnForInward");

        InwardInventory ii = inwardInventoryRepo.findById(inwardId)
                .orElseThrow(() ->
                        new Exception("Inward Inventory not found with id=" + inwardId)
                );

        editAuthorizationService.validateRejectReturnDate(ii.getDate());

        // ------------------------------------------------
        // Capture old quantities for async delta sync
        // ------------------------------------------------
        Map<String, Double> oldQuantityMap = new HashMap<>();
        for (InwardOutwardList io : ii.getInwardOutwardList()) {
            oldQuantityMap.put(io.getLineItemCode(), io.getQuantity());
        }

        Set<RejectInwardList> rejectInwardList = ii.getRejectInwardList();
        Set<InwardOutwardList> inwardOutwardListSet = ii.getInwardOutwardList();

        // ------------------------------------------------
        // 🔑 Pick BEST inward row (max qty that can satisfy)
        // ------------------------------------------------
        InwardOutwardList target = inwardOutwardListSet.stream()
                .filter(io -> io.getProduct().getProductId().equals(productId))
                .filter(io -> io.getQuantity() >= quantity)
                .max(Comparator.comparing(InwardOutwardList::getQuantity))
                .orElseThrow(() -> new Exception(
                        "Reject quantity cannot be greater than existing quantity for product - "
                                + productId
                ));

        Long warehouseId = target.getWarehouse().getWarehouseId();
        Double currentQuantity = target.getQuantity();

        // -----------------------------------------
        // Reduce batch qty for rejected inward — MUST run before stock update
        // so we can fail fast if batch qty was already consumed via outward.
        // If user specified batches (multi-batch inward) — drain those; else auto-drain last first
        // -----------------------------------------
        if (overrideBatches != null && !overrideBatches.isEmpty()) {
            // Filter valid entries and check for duplicates
            List<com.ec.application.data.BatchOverrideEntry> validOverrides = overrideBatches.stream()
                    .filter(e -> e.getBatchId() != null && e.getQty() != null && e.getQty() > 0)
                    .collect(Collectors.toList());
            java.util.Set<Long> seenBatchIds = new java.util.HashSet<>();
            for (com.ec.application.data.BatchOverrideEntry e : validOverrides) {
                if (!seenBatchIds.add(e.getBatchId())) {
                    throw new Exception("Duplicate batch ID " + e.getBatchId() + " in rejection entries.");
                }
            }
            // Validate sum of override qtys equals reject quantity
            double overrideTotal = validOverrides.stream()
                    .mapToDouble(e -> e.getQty()).sum();
            if (Math.abs(overrideTotal - quantity) > 0.001) {
                throw new Exception("Batch quantities (" + overrideTotal
                        + ") must equal the reject quantity (" + quantity + ").");
            }
            for (com.ec.application.data.BatchOverrideEntry entry : validOverrides) {
                InventoryBatch rb = inventoryBatchRepository.findById(entry.getBatchId())
                        .orElseThrow(() -> new Exception("Batch not found: " + entry.getBatchId()));
                // Product ownership
                if (!rb.getProduct().getProductId().equals(productId)) {
                    throw new Exception("Batch #" + entry.getBatchId() + " belongs to product '"
                            + rb.getProduct().getProductName() + "', not the rejected product.");
                }
                // Warehouse ownership
                if (!rb.getWarehouse().getWarehouseId().equals(warehouseId)) {
                    throw new Exception("Batch #" + entry.getBatchId() + " belongs to warehouse '"
                            + rb.getWarehouse().getWarehouseName() + "', not the inward's warehouse.");
                }
                // Inward ownership — batch must belong to this specific inward
                if (!rb.getInwardId().equals(inwardId)) {
                    throw new Exception("Batch #" + entry.getBatchId()
                            + " belongs to inward #" + rb.getInwardId()
                            + ", not the inward being rejected (#" + inwardId + ").");
                }
                if (entry.getQty() > rb.getQtyRemaining() + 0.001) {
                    throw new Exception("Cannot reject. Batch #" + entry.getBatchId()
                            + " has only " + String.format("%.3f", rb.getQtyRemaining())
                            + " units remaining — some qty was already consumed via outward.");
                }
                rb.setQtyRemaining(rb.getQtyRemaining() - entry.getQty());
                inventoryBatchRepository.save(rb);
            }
        } else {
            List<InventoryBatch> rejectBatches = inventoryBatchRepository.findAllByInwardIdAndProductId(inwardId, productId);
            if (!rejectBatches.isEmpty()) {
                if (rejectBatches.size() > 1) {
                    // Multiple batches — cannot auto-drain; user must specify which batch(es)
                    String pName = rejectBatches.get(0).getProduct() != null
                            ? rejectBatches.get(0).getProduct().getProductName() : "product";
                    throw new Exception(
                            "'" + pName + "' has " + rejectBatches.size() + " batches in this inward. "
                            + "Please specify which batch(es) the rejection of " + quantity
                            + " units should come from.");
                }
                // Single batch — auto drain
                InventoryBatch rb = rejectBatches.get(0);
                if (quantity > rb.getQtyRemaining() + 0.001) {
                    throw new Exception("Cannot reject " + quantity
                            + " units. Only " + String.format("%.3f", rb.getQtyRemaining())
                            + " units remain in the batch — the rest has already been consumed via outward.");
                }
                rb.setQtyRemaining(rb.getQtyRemaining() - quantity);
                inventoryBatchRepository.save(rb);
            }
        }

        // -----------------------------------------
        // Stock OUTWARD — runs only after batch validation passes
        // -----------------------------------------
        Double closingStock = stockService.updateStock(
                productId,
                warehouseId,
                quantity,
                "outward"
        );

        // -----------------------------------------
        // Record reject entry
        // -----------------------------------------
        rejectInwardList.add(
                new RejectInwardList(
                        new Date(),
                        target.getProduct(),
                        currentQuantity,
                        quantity,
                        closingStock,
                        remarks
                )
        );

        // -----------------------------------------
        // Reduce inward quantity
        // -----------------------------------------
        target.setQuantity(currentQuantity - quantity);
        target.setClosingStock(closingStock);

        ii.setRejectInwardList(rejectInwardList);
        ii.setInwardOutwardList(inwardOutwardListSet);
        inwardInventoryRepo.save(ii);

        // ------------------------------------------------
        // 🔁 Async indent / PO reconciliation
        // ------------------------------------------------
        if (Boolean.TRUE.equals(ii.getCreatedFromPO())) {

            List<IndentInwardDeltaDTO> deltas = new ArrayList<>();

            for (InwardOutwardList io : ii.getInwardOutwardList()) {

                Double oldQty = oldQuantityMap.get(io.getLineItemCode());
                Double newQty = io.getQuantity();

                if (oldQty == null || Double.compare(oldQty, newQty) != 0) {
                    deltas.add(
                            new IndentInwardDeltaDTO(
                                    io.getLineItemCode(),
                                    newQty
                            )
                    );
                }
            }

            if (!deltas.isEmpty()) {

                IndentInwardSyncDTO syncDTO = new IndentInwardSyncDTO(
                        ii.getDate(),
                        ThreadLocalStorage.getTenantName(),
                        ii.getInwardId(),
                        InwardActionType.UPDATE,
                        ii.getPurchaseOrderNo(),
                        deltas
                );

                applicationEventPublisher.publishEvent(new InwardSyncEvent(this, syncDTO, "reject-inward"));
            }
        }
    }

    private void setFieldsForInward(InwardInventory inwardInventory, InwardInventoryData iiData) throws
            Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        inwardInventory.setInvoiceReceived(iiData.getInvoiceReceived());
        inwardInventory.setDate(iiData.getInwardDate());
        inwardInventory.setOurSlipNo(mrnService.getNextMrn().toString());
        inwardInventory.setVehicleNo(iiData.getVehicleNo());
        inwardInventory.setSupplierSlipNo(iiData.getSupplierSlipNo());
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
        inwardInventory.setNoChallanBillReason(iiData.getNoChallanBillReason());
        inwardInventory.setIsSampleInward(
                iiData.getIsSampleInward() != null && iiData.getIsSampleInward()
        );
        inwardInventory.setCreatedFromPO(false);
    }

    public Set<InwardOutwardList> fetchInwardOutwardList(List<ProductWithQuantity> productWithQuantities) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();
        for (ProductWithQuantity productWithQuantity : productWithQuantities) {
            InwardOutwardList inwardOutwardList = new InwardOutwardList();
            Product product = productRepo.findById(productWithQuantity.getProductId()).get();
            boolean hasSplitsDirect = productWithQuantity.getBatchSplits() != null && !productWithQuantity.getBatchSplits().isEmpty();
            if (product.isBatchTracked()) {
                if (!hasSplitsDirect && product.requiresExpiry() && productWithQuantity.getExpiryDate() == null)
                    throw new IllegalArgumentException(
                            "Expiry date is required for product: '" + product.getProductName() + "'");
                if (hasSplitsDirect) validateBatchSplits(productWithQuantity.getBatchSplits(), productWithQuantity.getQuantity(), product.getProductName(), product.requiresExpiry());
            }
            inwardOutwardList.setProduct(product);
            inwardOutwardList.setQuantity(productWithQuantity.getQuantity());
            inwardOutwardList.setBrand(hasSplitsDirect ? productWithQuantity.getBatchSplits().get(0).getBrand() : productWithQuantity.getBrand());
            inwardOutwardList.setExpiryDate(hasSplitsDirect ? productWithQuantity.getBatchSplits().get(0).getExpiryDate() : productWithQuantity.getExpiryDate());
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
        boolean isSampleInward = Boolean.TRUE.equals(iiData.getIsSampleInward());

        for (ProductWithQuantity productWithQuantity : iiData.getProductWithQuantities()) {
            if (!productRepo.existsById(productWithQuantity.getProductId()))
                throw new Exception("Product not found with ID " + productWithQuantity.getProductId());

            // Guard: regular direct inward must only use unmanaged products.
            // Sample inward is exempt — it can receive any product without a PO.
            if (!isSampleInward) {
                Product product = productRepo.findById(productWithQuantity.getProductId())
                        .orElseThrow(() -> new Exception("Product not found"));
                if (Boolean.TRUE.equals(product.getIsManagedInventory())) {
                    throw new Exception(
                            "Product '" + product.getProductName() + "' is a managed inventory product " +
                                    "and cannot be inwarded directly without a Purchase Order. " +
                                    "Use 'Sample Inward' if this is a sample receipt."
                    );
                }
            }

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

        boolean noChallan = iiData.getChallanNo() == null || iiData.getChallanNo().trim().isEmpty();
        boolean noBill = iiData.getBillNo() == null || iiData.getBillNo().trim().isEmpty();
        if (noChallan && noBill) {
            if (iiData.getNoChallanBillReason() == null || iiData.getNoChallanBillReason().trim().isEmpty())
                throw new Exception("Please provide a reason since both Challan No. and Bill No. are missing.");
        }
    }

    public List<ProductGroupedDAO> getTotalsForInward(FilterDataList filterDataList) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Specification<InwardInventory> spec = InwardInventorySpecification.getSpecification(filterDataList);
        if (spec != null)
            return fetchGroupingForFilteredData(spec);
        else
            return fetchInwardnventoryGroupBy();
    }

    public List<InwardInventoryExportDAO2> fetchInwardnventoryForExport2(FilterDataList filterDataList) throws
            Exception {
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
        Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);

        if (!inwardInventoryOpt.isPresent()) {
            throw new Exception("Inward Inventory with ID not found");
        }

        InwardInventory inwardInventory = inwardInventoryOpt.get();
        editAuthorizationService.validateDeleteDate(inwardInventory.getDate());
        if (inwardInventory.getPurchaseOrderNo() != null &&
                purchaseOrderShortClosedViewRepo.existsById(inwardInventory.getPurchaseOrderNo())) {
            throw new Exception("Cannot delete inward inventory linked to a short closed purchase order.");
        }

        updateStockBeforeDelete(inwardInventory);
        removeOrphans(inwardInventory);
        InwardSnapshot snapshot = buildSnapshot(inwardInventory);

        if (inwardInventory.getCreatedFromPO()) {
            List<IndentInwardDeltaDTO> deltas = new ArrayList<>();
            for (InwardOutwardList io : inwardInventory.getInwardOutwardList()) {
                deltas.add(new IndentInwardDeltaDTO(io.getLineItemCode(), -io.getQuantity()));
            }
            IndentInwardSyncDTO syncDTO = new IndentInwardSyncDTO(inwardInventory.getDate(), ThreadLocalStorage.getTenantName(), inwardInventory.getInwardId(), InwardActionType.DELETE, inwardInventory.getPurchaseOrderNo(), deltas);
            indentInventoryAsyncUpdater.updateIndentAfterInwardAsync(syncDTO, "delete");
        }
        zeroBatchesForDeletedInward(id);
        inwardInventoryRepo.softDeleteById(id);
        String deleteUser = resolveCurrentUser();
        activityLogService.record("DELETED", "INWARD", String.valueOf(id),
                ActivityLogDescription.of("Inward " + id + " deleted by " + deleteUser),
                deleteUser);
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

    /*
        @Transactional(rollbackFor = Exception.class)
        public InwardInventory updateInwardnventory(InwardInventoryData iiData, Long id) throws Exception {
            logger.info("In undate inward inventory flow");
            Optional<InwardInventory> inwardInventoryOpt = inwardInventoryRepo.findById(id);
            if (!inwardInventoryOpt.isPresent())
                throw new Exception("Inventory Entry with ID not found");
            return null;
        }
    */
    private InwardSnapshot buildSnapshot(InwardInventory inward) {
        InwardSnapshot snap = new InwardSnapshot();
        snap.setInwardId(inward.getInwardId());
        snap.setInwardDate(inward.getDate());
        for (InwardOutwardList io : inward.getInwardOutwardList()) {
            InwardLineSnapshot line = new InwardLineSnapshot();
            line.setLineItemCode(io.getLineItemCode());
            line.setQuantity(io.getQuantity());
            snap.getLines().add(line);
        }
        return snap;
    }

    private void createBatchesForInward(InwardInventory inwardInventory, Map<Long, List<InwardBatchSplit>> splitsByProductId) {
        for (InwardOutwardList iol : inwardInventory.getInwardOutwardList()) {
            if (!iol.getProduct().isBatchTracked()) continue;
            Long productId = iol.getProduct().getProductId();
            List<InwardBatchSplit> splits = splitsByProductId != null ? splitsByProductId.get(productId) : null;
            if (splits != null && !splits.isEmpty()) {
                for (InwardBatchSplit split : splits) {
                    InventoryBatch batch = new InventoryBatch();
                    batch.setProduct(iol.getProduct());
                    batch.setWarehouse(iol.getWarehouse());
                    batch.setInwardId(inwardInventory.getInwardId());
                    batch.setBrand(split.getBrand());
                    batch.setLotNumber(split.getLotNumber());
                    batch.setExpiryDate(split.getExpiryDate());
                    batch.setReceivedDate(inwardInventory.getDate());
                    batch.setQtyReceived(split.getQty());
                    batch.setQtyRemaining(split.getQty());
                    inventoryBatchRepository.save(batch);
                }
            } else {
                // Single-batch fallback (used only when NONE products have no splits,
                // or legacy PO path without splits for batch-tracked product)
                InventoryBatch batch = new InventoryBatch();
                batch.setProduct(iol.getProduct());
                batch.setWarehouse(iol.getWarehouse());
                batch.setInwardId(inwardInventory.getInwardId());
                batch.setBrand(iol.getBrand());
                batch.setExpiryDate(iol.getExpiryDate());
                batch.setReceivedDate(inwardInventory.getDate());
                batch.setQtyReceived(iol.getQuantity());
                batch.setQtyRemaining(iol.getQuantity());
                inventoryBatchRepository.save(batch);
            }
        }
    }

    private IndentsForInwardView fetchPoLine(String lineItemCode, String tenant) {

        List<IndentsForInwardView> result = indentsForInwardViewRepository.getLineItemDetails(lineItemCode, tenant);

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "No PO line item found for code: " + lineItemCode);
        }

        return result.get(0);
    }

    private void validateQuantityAgainstPO(String lineItemCode, String tenant, Double newQty, Double oldQty) {

        IndentsForInwardView poView = fetchPoLine(lineItemCode, tenant);

        double orderedQty = poView.getQuantity() != null ? poView.getQuantity() : 0d;
        double totalInwardQty = poView.getTotalInwardQuantity() != null ? poView.getTotalInwardQuantity() : 0d;
        double effectiveAlreadyInwarded = oldQty == null ? totalInwardQty : totalInwardQty - oldQty;
        double maxAllowed = orderedQty - effectiveAlreadyInwarded;

        if (newQty > maxAllowed) {
            throw new IllegalStateException(
                    String.format(
                            "Quantity exceeds PO limit for lineItemCode [%s]. " +
                                    "Ordered: %.2f, Already inwarded: %.2f, Max allowed now: %.2f",
                            lineItemCode,
                            orderedQty,
                            effectiveAlreadyInwarded,
                            maxAllowed
                    )
            );
        }
    }

    private void validateBatchSplits(List<InwardBatchSplit> splits, Double totalQty, String productName, boolean requireExpiry) {
        double splitSum = splits.stream().mapToDouble(s -> s.getQty() != null ? s.getQty() : 0.0).sum();
        if (Math.abs(splitSum - totalQty) > 0.001)
            throw new IllegalArgumentException(
                    "Batch split quantities (" + splitSum + ") must equal total quantity (" + totalQty + ") for product: '" + productName + "'");
        for (InwardBatchSplit split : splits) {
            if (split.getQty() == null || split.getQty() <= 0)
                throw new IllegalArgumentException(
                        "Each batch split quantity must be greater than zero for product: '" + productName + "'");
            if (requireExpiry && split.getExpiryDate() == null)
                throw new IllegalArgumentException(
                        "Expiry date is required for each batch split of product: '" + productName + "'");
        }
    }

    private void reconcileBatchesForEditedInward(InwardInventory inward, List<ProductAndQuantity> productWithQuantities) {
        Map<Long, ProductAndQuantity> paqByProductId = productWithQuantities.stream()
                .collect(Collectors.toMap(ProductAndQuantity::getProductId, p -> p));

        for (InwardOutwardList iol : inward.getInwardOutwardList()) {
            Long productId = iol.getProduct().getProductId();
            if (!iol.getProduct().isBatchTracked()) continue;
            boolean requireExpiry = iol.getProduct().requiresExpiry();

            ProductAndQuantity paq = paqByProductId.get(productId);
            if (paq == null) continue;

            Double newTotal = paq.getQuantity();
            List<InventoryBatch> batches = inventoryBatchRepository.findAllByInwardIdAndProductId(inward.getInwardId(), productId);

            if (batches.isEmpty()) {
                // Pre-tracking inward — no batch records exist yet.
                // Skip: use "Split Existing Stock" on the Stock page instead.
                continue;
            }

            double existingTotal = batches.stream().mapToDouble(InventoryBatch::getQtyReceived).sum();
            double delta = newTotal - existingTotal;
            if (Math.abs(delta) < 0.001) {
                // Qty unchanged — but user may have supplied updated batch metadata (brand / lot / expiry).
                // Update existing batches positionally: split[0] → batches[0], split[1] → batches[1], …
                boolean hasSplits = paq.getBatchSplits() != null && !paq.getBatchSplits().isEmpty();
                if (hasSplits) {
                    List<InwardBatchSplit> splits = paq.getBatchSplits();
                    for (int i = 0; i < Math.min(splits.size(), batches.size()); i++) {
                        InwardBatchSplit split = splits.get(i);
                        InventoryBatch batch = batches.get(i);
                        if (split.getBrand() != null)      batch.setBrand(split.getBrand());
                        if (split.getLotNumber() != null)  batch.setLotNumber(split.getLotNumber());
                        if (split.getExpiryDate() != null) batch.setExpiryDate(split.getExpiryDate());
                        inventoryBatchRepository.save(batch);
                    }
                }
                continue;
            }

            if (delta < 0) {
                double reduction = Math.abs(delta);
                String productName = iol.getProduct().getProductName();

                if (batches.size() == 1) {
                    // Single batch — auto reduce (we know exactly which batch)
                    InventoryBatch batch = batches.get(0);
                    double consumed = batch.getQtyReceived() - batch.getQtyRemaining();
                    double actualReduce = Math.min(batch.getQtyRemaining(), reduction);
                    batch.setQtyRemaining(batch.getQtyRemaining() - actualReduce);
                    batch.setQtyReceived(Math.max(batch.getQtyReceived() - actualReduce, consumed));
                    reduction -= actualReduce;
                    inventoryBatchRepository.save(batch);
                    if (reduction > 0.001) {
                        double alreadyConsumed = Math.abs(delta) - reduction;
                        throw new IllegalArgumentException(
                                "Cannot reduce inward quantity by " + Math.abs(delta) + " for product '"
                                + productName + "'. Only "
                                + String.format("%.3f", alreadyConsumed)
                                + " units remain in the batch — the rest has already been consumed via outward.");
                    }
                } else {
                    // Multiple batches — user must specify which batches to reduce via batchSplits with batchId
                    boolean hasSplitsWithBatchId = paq.getBatchSplits() != null
                            && !paq.getBatchSplits().isEmpty()
                            && paq.getBatchSplits().stream().anyMatch(s -> s.getBatchId() != null);
                    if (!hasSplitsWithBatchId) {
                        throw new IllegalArgumentException(
                                "Product '" + productName + "' has " + batches.size()
                                + " batches in this inward. Please specify which batch(es) the quantity reduction of "
                                + String.format("%.3f", reduction)
                                + " units should come from using the 'Edit Batches' button.");
                    }
                    // Validate sum of splits equals reduction
                    double splitTotal = paq.getBatchSplits().stream()
                            .filter(s -> s.getBatchId() != null && s.getQty() != null)
                            .mapToDouble(InwardBatchSplit::getQty).sum();
                    if (Math.abs(splitTotal - reduction) > 0.001) {
                        throw new IllegalArgumentException(
                                "Batch reduction quantities (" + splitTotal + ") must equal the quantity reduction ("
                                + String.format("%.3f", reduction) + ") for product '" + productName + "'.");
                    }
                    // Apply reduction per specified batch
                    for (InwardBatchSplit split : paq.getBatchSplits()) {
                        if (split.getBatchId() == null || split.getQty() == null || split.getQty() <= 0) continue;
                        InventoryBatch batch = batches.stream()
                                .filter(b -> b.getBatchId().equals(split.getBatchId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Batch #" + split.getBatchId() + " not found in this inward for product '" + productName + "'."));
                        if (split.getQty() > batch.getQtyRemaining() + 0.001) {
                            throw new IllegalArgumentException(
                                    "Cannot reduce batch #" + split.getBatchId() + " by " + split.getQty()
                                    + " — only " + String.format("%.3f", batch.getQtyRemaining())
                                    + " units remaining (the rest was consumed via outward).");
                        }
                        double consumed = batch.getQtyReceived() - batch.getQtyRemaining();
                        batch.setQtyRemaining(batch.getQtyRemaining() - split.getQty());
                        batch.setQtyReceived(Math.max(batch.getQtyReceived() - split.getQty(), consumed));
                        inventoryBatchRepository.save(batch);
                    }
                }
            } else {
                // delta > 0 — increase: user MUST specify batch splits for the new quantity
                boolean hasSplits = paq.getBatchSplits() != null && !paq.getBatchSplits().isEmpty();
                if (!hasSplits) {
                    throw new IllegalArgumentException(
                            "Please specify which batch the increased quantity of "
                            + String.format("%.3f", delta) + " units belongs to for product '"
                            + iol.getProduct().getProductName()
                            + "' using the 'Edit Batches' button.");
                }
                validateBatchSplits(paq.getBatchSplits(), delta, iol.getProduct().getProductName(), requireExpiry);
                for (InwardBatchSplit split : paq.getBatchSplits()) {
                    InventoryBatch last = batches.get(batches.size() - 1);
                    // Merge into existing batch only if expiry AND brand AND lot match
                    boolean sameBatch = split.getQty() != null
                            && objectsEqual(split.getExpiryDate(), last.getExpiryDate())
                            && objectsEqual(split.getBrand(), last.getBrand())
                            && objectsEqual(split.getLotNumber(), last.getLotNumber());
                    if (sameBatch) {
                        last.setQtyReceived(last.getQtyReceived() + split.getQty());
                        last.setQtyRemaining(last.getQtyRemaining() + split.getQty());
                        inventoryBatchRepository.save(last);
                    } else {
                        InventoryBatch nb = new InventoryBatch();
                        nb.setProduct(iol.getProduct()); nb.setWarehouse(iol.getWarehouse());
                        nb.setInwardId(inward.getInwardId()); nb.setBrand(split.getBrand());
                        nb.setLotNumber(split.getLotNumber());
                        nb.setExpiryDate(split.getExpiryDate()); nb.setReceivedDate(inward.getDate());
                        nb.setQtyReceived(split.getQty()); nb.setQtyRemaining(split.getQty());
                        inventoryBatchRepository.save(nb);
                    }
                }
            }
        }
    }

    private void zeroBatchesForDeletedInward(Long inwardId) throws Exception {
        List<InventoryBatch> batches = inventoryBatchRepository.findAllByInwardId(inwardId);
        // Guard: if any batch from this inward was partially consumed (via outward or stock adjustment),
        // deleting would orphan OutwardBatchConsumption records and corrupt batch vs stock totals.
        for (InventoryBatch batch : batches) {
            double consumed = batch.getQtyReceived() - batch.getQtyRemaining();
            if (consumed > 0.001) {
                throw new Exception(
                    "Cannot delete this inward. Batch #" + batch.getBatchId()
                    + " (product: " + batch.getProduct().getProductName() + ")"
                    + " has already been partially consumed via outward or stock adjustment ("
                    + String.format("%.3f", consumed) + " units consumed). "
                    + "Please reverse those records first before deleting this inward.");
            }
        }
        for (InventoryBatch batch : batches) {
            batch.setQtyRemaining(0.0);
            batch.setDeleted(true);
            inventoryBatchRepository.save(batch);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InwardInventory createOpeningStockInward(OpeningStockInwardDTO dto) throws Exception {
        log.info("Invoked createOpeningStockInward for tenant={}", ThreadLocalStorage.getTenantName());

        if (dto.getInwardDate() == null)
            throw new IllegalArgumentException("inwardDate is required.");

        if (dto.getLineItems() == null || dto.getLineItems().isEmpty())
            throw new IllegalArgumentException("At least one line item is required.");

        // --- Duplicate check within the request ---
        long duplicateCount = dto.getLineItems().stream()
                .collect(Collectors.groupingBy(
                        li -> li.getProductName().trim().toLowerCase()
                                + "_" + li.getWarehouseName().trim().toLowerCase(),
                        counting()))
                .values().stream()
                .filter(count -> count > 1)
                .count();

        if (duplicateCount > 0)
            throw new IllegalArgumentException(
                    "Duplicate product-warehouse combination found in request. " +
                            "Each product must appear once per warehouse.");

        // --- COLLECT ALL ERRORS FIRST before any processing ---
        List<String> errors = new ArrayList<>();

        for (OpeningStockInwardDTO.OpeningStockLineItem lineItem : dto.getLineItems()) {

            if (lineItem.getProductName() == null || lineItem.getProductName().trim().isEmpty()) {
                errors.add("productName cannot be blank.");
                continue;
            }
            if (lineItem.getWarehouseName() == null || lineItem.getWarehouseName().trim().isEmpty()) {
                errors.add("warehouseName cannot be blank for product: " + lineItem.getProductName());
                continue;
            }
            if (lineItem.getQuantity() == null || lineItem.getQuantity() <= 0) {
                errors.add("quantity must be greater than zero for product: " + lineItem.getProductName());
                continue;
            }

            Product product = productRepo.findByProductName(lineItem.getProductName().trim());
            if (product == null) {
                errors.add("Product not found: '" + lineItem.getProductName() + "'");
                continue;
            }

            List<Warehouse> warehouses = warehouseRepo.findByName(lineItem.getWarehouseName().trim());
            if (warehouses == null || warehouses.isEmpty()) {
                errors.add("Warehouse not found: '" + lineItem.getWarehouseName() + "'");
                continue;
            }

            int existingCount = inwardInventoryRepo.countOpeningStockForProductAndWarehouse(
                    product.getProductName(), warehouses.get(0).getWarehouseName());
            if (existingCount > 0) {
                errors.add("Opening stock already exists for product '" + lineItem.getProductName() +
                        "' in warehouse '" + lineItem.getWarehouseName() + "'");
            }
        }

        // --- If any errors, throw them ALL at once before touching any data ---
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Opening stock creation failed with " + errors.size() + " error(s):\n" +
                            String.join("\n", errors));
        }

        // --- All validations passed — now resolve and build line items ---
        SystemContact systemContact = systemContactRepo.findByNameIgnoreCase("OPENING STOCK")
                .orElseThrow(() -> new IllegalStateException(
                        "Opening Stock system contact not found for this tenant."));

        Supplier supplier = supplierRepo.findByIdUnfiltered(systemContact.getContactId())
                .orElseThrow(() -> new IllegalStateException(
                        "Could not load Opening Stock contact as Supplier reference."));

        Set<InwardOutwardList> inwardOutwardListSet = new HashSet<>();

        for (OpeningStockInwardDTO.OpeningStockLineItem lineItem : dto.getLineItems()) {
            Product product = productRepo.findByProductName(lineItem.getProductName().trim());
            List<Warehouse> warehouses = warehouseRepo.findByName(lineItem.getWarehouseName().trim());
            Warehouse warehouse = warehouses.get(0);

            InwardOutwardList iol = new InwardOutwardList();
            iol.setProduct(product);
            iol.setWarehouse(warehouse);
            iol.setQuantity(lineItem.getQuantity());
            iol.setLineItemCode(null);
            inwardOutwardListSet.add(iol);
        }

        InwardInventory inwardInventory = new InwardInventory();
        inwardInventory.setDate(dto.getInwardDate());
        inwardInventory.setSupplier(supplier);
        inwardInventory.setInvoiceReceived(false);
        inwardInventory.setCreatedFromPO(false);
        inwardInventory.setPurchaseOrderNo(null);
        inwardInventory.setPurchaseOrderDate(null);
        inwardInventory.setVehicleNo(null);
        inwardInventory.setSupplierSlipNo(null);
        inwardInventory.setChallanNo(null);
        inwardInventory.setChallanDate(null);
        inwardInventory.setBillNo(null);
        inwardInventory.setBillDate(null);
        inwardInventory.setAdditionalInfo("Opening Stock Entry");
        inwardInventory.setOurSlipNo(mrnService.getNextMrn().toString());
        inwardInventory.setInwardOutwardList(inwardOutwardListSet);

        updateStockForCreateInwardInventory(inwardInventory);
        inwardInventoryRepo.save(inwardInventory);

        log.info("Opening stock inward created. InwardId={}, MRN={}, Products={}",
                inwardInventory.getInwardId(),
                inwardInventory.getOurSlipNo(),
                inwardInventory.getInwardOutwardList().size());

        return inwardInventory;
    }

    /** Null-safe equals used when deciding whether to merge into existing batch. */
    private static boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
