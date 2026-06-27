package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.PurchaseOrderSpecification;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.*;
import com.ec.application.data.*;
import com.ec.application.enricher.PurchaseOrderUiEnricher;
import com.ec.application.indentpo.PurchaseOrderCompletionEvaluator;
import com.ec.application.indentpo.PurchaseOrderLifecycleManager;
import com.ec.application.model.*;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.PurchaseOrderCustomChargeRepo;
import com.ec.application.repository.PurchaseOrderLineRepository;
import com.ec.application.repository.PurchaseOrderRepo;
import java.util.stream.Collectors;
import com.ec.application.util.PurchaseOrderPriceMasker;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.checkerframework.checker.units.qual.A;
import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderService extends ReusableFields {

    @Autowired
    PurchaseOrderRepo purchaseOrderRepo;

    @Autowired
    PurchaseOrderCustomChargeRepo customChargeRepo;

    @Autowired
    PurchaseOrderValidator validator;

    @Autowired
    PurchaseOrderBuilder poBuilder;

    @Autowired
    IndentStatusUpdater indentStatusUpdater;

    @Autowired
    PurchaseOrderUiEnricher purchaseOrderUiEnricher;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    PurchaseOrderLifecycleManager poLifecycleManager;

    @Autowired
    DraftService draftService;

    @Autowired
    PurchaseOrderPriceMasker purchaseOrderPriceMasker;

    @Autowired
    PurchaseOrderStatusHistoryService poStatusHistoryService;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    ActivityLogService activityLogService;

    @Autowired
    IndentInventoryListRepo indentInventoryListRepo;

    @Autowired
    DBFileStorageService dbFileStorageService;

    @Autowired
    FirmService firmService;

    @Autowired
    SupplierService supplierService;

    @Autowired
    TenantService tenantService;

    @Autowired
    PurchaseOrderCompletionEvaluator poCompletionEvaluator;

    @Autowired
    PurchaseOrderLineRepository purchaseOrderLineRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePoRequest request) throws Exception {
        validator.validateIndentLineItems(request.getLineItems());
        validator.validateOverridePhoneNumber(request.getOverridePhoneNumber());
        validator.validateOverrideEmail(request.getOverrideEmail());
        validatePoDateBackdating(request.getPoDate());
        if (request.getProjectName() == null || request.getProjectName().trim().isEmpty())
            throw new Exception("Project is a mandatory field");
        PurchaseOrder po = poBuilder.buildPurchaseOrder(request);
        PurchaseOrder savedPO = purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(savedPO, POIndentUpdateAction.CREATE_PO);
        draftService.deleteDraftForUser("PO");
        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(savedPO, null, savedPO.getStatus(), username, buildPoCreationMessage(request, username), buildPoCreationRelations(request));
        String activityUser = resolveCurrentUser();
        activityLogService.record("CREATED", "PURCHASE_ORDER", savedPO.getPurchaseOrderId(),
                "Purchase Order " + savedPO.getPurchaseOrderId() + " created by " + activityUser, activityUser);
        return savedPO;
    }

    @Transactional
    public PurchaseOrder updatePurchaseOrder(String id, UpdatePoRequest request) throws Exception {
        validator.validateOverridePhoneNumber(request.getOverridePhoneNumber());
        validator.validateOverrideEmail(request.getOverrideEmail());

        PurchaseOrder po = purchaseOrderRepo.findById(id)
                .orElseThrow(() -> new Exception("Purchase Order not found: " + id));

        boolean isAdmin = userDetailsService.getCurrentUser().getRoles().stream()
                .anyMatch(r -> r.toLowerCase().contains(RoleConstants.ADMIN));
        if (!isAdmin && !POStatusConstants.STATUS_NEW.equals(po.getStatus())) {
            throw new Exception("Purchase Order cannot be edited. Only POs in NEW status can be edited.");
        }

        // Update header fields
        if (request.getSupplierId() != null) {
            po.setSupplier(supplierService.findSingleSupplier(request.getSupplierId()));
        }
        if (request.getFirmId() != null) {
            po.setFirm(firmService.findSingleFirm(request.getFirmId()));
        }
        if (request.getPoDate() != null) {
            po.setPoDate(request.getPoDate());
        }
        po.setSubject(request.getSubject());
        po.setNotes(request.getNotes());
        po.setOverridePhoneNumber(request.getOverridePhoneNumber());
        po.setOverrideEmail(request.getOverrideEmail());
        po.setProjectName(request.getProjectName());
        po.setSpecialPo(request.isSpecialPo());
        po.setFreightCharges(request.getFreightCharges());
        po.setFreightGstPercent(request.getFreightGstPercent());
        po.setTotalFreightCharges(request.getTotalFreightCharges());
        po.setPoDiscount(request.getPoDiscount());
        po.setGrandTotal(request.getGrandTotal());

        po.getCustomCharges().clear();
        if (request.getCustomCharges() != null) {
            for (CustomChargeRequest chargeReq : request.getCustomCharges()) {
                PurchaseOrderCustomCharge charge = new PurchaseOrderCustomCharge();
                charge.setPurchaseOrder(po);
                charge.setChargeName(chargeReq.getChargeName());
                charge.setChargeAmount(chargeReq.getChargeAmount());
                charge.setChargeGstPercent(chargeReq.getChargeGstPercent());
                charge.setTotalChargeAmount(chargeReq.getTotalChargeAmount());
                po.getCustomCharges().add(charge);
            }
        }

        if (request.getFileInformations() != null) {
            po.setFileInformations(ReusableMethods.convertFilesListToSet(request.getFileInformations()));
        }

        // Update existing lines — quantity and indent refs are NOT changed
        if (request.getLineUpdates() != null) {
            Map<Long, UpdatePoLineRequest> lineUpdateMap = request.getLineUpdates().stream()
                    .filter(u -> u.getLineId() != null)
                    .collect(Collectors.toMap(UpdatePoLineRequest::getLineId, u -> u));

            for (PurchaseOrderLine line : po.getLines()) {
                UpdatePoLineRequest update = lineUpdateMap.get(line.getId());
                if (update != null) {
                    line.setRate(update.getRate());
                    line.setDiscountPercent(update.getDiscountPercent());
                    line.setTolerancePercent(update.getTolerancePercent() != null ? update.getTolerancePercent() : 0.0);
                    line.setGstPercent(update.getGstPercent());
                    line.setBrand(update.getBrand());
                    line.setGrade(update.getGrade());
                    line.setDiameter(update.getDiameter());
                    line.setSpecification(update.getSpecification());
                    line.setNetRate(update.getNetRate());
                    line.setTotalAmount(update.getTotalAmount());
                    line.setSampleImageFileId(update.getSampleImageFileId());
                }
            }
        }

        PurchaseOrder saved = purchaseOrderRepo.save(po);
        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(saved, saved.getStatus(), saved.getStatus(), username,
                "Purchase Order updated by " + username, null);
        String activityUser = resolveCurrentUser();
        activityLogService.record("UPDATED", "PURCHASE_ORDER", saved.getPurchaseOrderId(),
                "Purchase Order " + saved.getPurchaseOrderId() + " updated by " + activityUser, activityUser);
        return getPurchaseOrderWithInit(saved.getPurchaseOrderId());
    }

    @Transactional(readOnly = true)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(FilterDataList filterDataList, Pageable pageable) throws Exception {

        ReturnPurchaseOrderData returnData = new ReturnPurchaseOrderData();
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.getSpecification(filterDataList);

        if (spec == null) {
            spec = Specification.where(null); // no-op spec
        }

        Page<PurchaseOrder> page = purchaseOrderRepo.findAll(spec, pageable);

        // Initialize lazy-loaded associations
        initializeLazyAssociations(page.getContent());

        // MASK PRICE FIELDS
        purchaseOrderPriceMasker.mask(page);

        // Enrich UI flags
        purchaseOrderUiEnricher.enrich(page.getContent());
        returnData.setPuchaseOrders(page);
        returnData.setPoDropdown(populateDropdownService.fetchData("purchaseorder"));
        return returnData;
    }

    private void initializeLazyAssociations(List<PurchaseOrder> purchaseOrders) {
        purchaseOrders.forEach(po -> {
            // Initialize supplier
            if (po.getSupplier() != null) {
                Hibernate.initialize(po.getSupplier());
                String supplierName = po.getSupplier().getName(); // Touch to load
            }

            // Initialize firm
            if (po.getFirm() != null) {
                Hibernate.initialize(po.getFirm());
                String firmName = po.getFirm().getFirmName(); // Touch to load
            }

            // Initialize lines
            if (po.getLines() != null && !po.getLines().isEmpty()) {
                Hibernate.initialize(po.getLines());
                po.getLines().forEach(line -> {
                    // Initialize product and its category (for lead time resolution)
                    if (line.getProduct() != null) {
                        Hibernate.initialize(line.getProduct());
                        String productName = line.getProduct().getProductName(); // Touch to load
                        if (line.getProduct().getCategory() != null) {
                            Hibernate.initialize(line.getProduct().getCategory());
                        }
                    }

                    // Initialize indent refs
                    if (line.getIndentRefs() != null) {
                        Hibernate.initialize(line.getIndentRefs());
                    }
                });
            }
        });
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderWithInit(String id) throws Exception {
        PurchaseOrder po = purchaseOrderRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase Order not found with ID: " + id
                        ));

        // Initialize supplier & firm
        if (po.getSupplier() != null) {
            po.getSupplier().getName();
        }
        if (po.getFirm() != null) {
            po.getFirm().getFirmName();
        }

        // Initialize lines + indentRefs + product
        for (PurchaseOrderLine line : po.getLines()) {
            line.getIndentRefs().size();
            if (line.getProduct() != null) {
                line.getProduct().getProductName();
            }
        }

        // Populate balanceQuantity, receivedQuantity and lineItemStatus from linked indent line items
        try {
            java.util.List<String> lineItemCodes = po.getLines().stream()
                    .flatMap(line -> line.getIndentRefs().stream())
                    .map(ref -> ref.getIndentLineItemCode())
                    .filter(code -> code != null && !code.isEmpty())
                    .collect(Collectors.toList());
            if (!lineItemCodes.isEmpty()) {
                java.util.List<IndentInventoryList> lineItems =
                        indentInventoryListRepo.findByLineItemCodeIn(lineItemCodes);

                // Build map: lineItemCode → IndentInventoryList for fast lookup
                java.util.Map<String, IndentInventoryList> lineItemMap = lineItems.stream()
                        .collect(Collectors.toMap(
                                IndentInventoryList::getLineItemCode,
                                li -> li,
                                (a, b) -> a));

                for (PurchaseOrderLine line : po.getLines()) {
                    if (POStatusConstants.STATUS_PARTIAL.equals(po.getStatus())) {
                        double received = line.getIndentRefs().stream()
                                .map(ref -> lineItemMap.get(ref.getIndentLineItemCode()))
                                .filter(li -> li != null && li.getQuantityReceived() != null)
                                .mapToDouble(IndentInventoryList::getQuantityReceived)
                                .sum();
                        line.setReceivedQuantity(received > 0 ? received : null);

                        double balance = line.getIndentRefs().stream()
                                .map(ref -> lineItemMap.get(ref.getIndentLineItemCode()))
                                .filter(li -> li != null && li.getQuantityPending() != null)
                                .mapToDouble(IndentInventoryList::getQuantityPending)
                                .sum();
                        line.setBalanceQuantity(balance > 0 ? balance : null);
                    }

                    // Derive line status from its first indent ref (most lines have one ref).
                    // If multiple refs, take the most "advanced" status to reflect true state.
                    String lineStatus = line.getIndentRefs().stream()
                            .map(ref -> lineItemMap.get(ref.getIndentLineItemCode()))
                            .filter(li -> li != null)
                            .map(IndentInventoryList::getLineItemStatus)
                            .reduce(null, (acc, s) -> {
                                if (acc == null) return s;
                                // Precedence: INWARD_COMPLETE > INWARD_PARTIAL > SHORT_CLOSED > CANCELLED > PO_CREATED > NEW
                                int accRank = statusRank(acc);
                                int sRank = statusRank(s);
                                return sRank > accRank ? s : acc;
                            });
                    line.setLineItemStatus(lineStatus);
                }

            }
        } catch (Exception e) {
            // Non-critical — just skip if it fails
        }

        // Pre-fetch sample image bytes for PDF generation.
        // Files are stored in master schema (FileHandlingService uses @UseDefaultTenant).
        for (PurchaseOrderLine line : po.getLines()) {
            if (line.getSampleImageFileId() != null) {
                try {
                    byte[] bytes = dbFileStorageService.getFileBytes(line.getSampleImageFileId());
                    line.setSampleImageData(bytes);
                } catch (Exception ex) {
                    // Non-critical — PDF will just show "-" for this line
                }
            }
        }

        // Enrich lead time and overdue flags per line
        purchaseOrderUiEnricher.enrichOverdueFlag(po);

        // MASK PRICE FIELDS
        purchaseOrderPriceMasker.mask(po);
        return po;
    }

    /**
     * Adds a new line item to an existing PO.
     * Allowed for POs in NEW or PARTIAL status only.
     * The supplied indent line item must be in NEW status (not yet linked to any PO).
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder addLineItem(String poId, CreatePoLineRequest req) throws Exception {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new Exception("Purchase Order not found: " + poId));

        validator.validateAddLineToPO(po);
        validator.validateIndentLineItems(Collections.singletonList(req));

        List<String> processedCodes = new ArrayList<>();

        Optional<PurchaseOrderLine> existing = po.getLines().stream()
                .filter(l -> l.getProduct() != null
                        && req.getProductId() != null
                        && l.getProduct().getProductId().equals(req.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            PurchaseOrderLine line = existing.get();
            if (req.getIndentRefs() == null || req.getIndentRefs().isEmpty()) {
                throw new Exception("Indent refs are required when clubbing into an existing PO line.");
            }
            double newQty = (line.getQuantity() != null ? line.getQuantity() : 0)
                    + (req.getQuantity() != null ? req.getQuantity() : 0);
            line.setQuantity(newQty);
            for (IndentLineRefRequest refReq : req.getIndentRefs()) {
                List<IndentInventoryList> items =
                        indentInventoryListRepo.findByLineItemCode(refReq.getIndentLineItemCode());
                if (items.isEmpty())
                    throw new Exception("Indent line item not found: " + refReq.getIndentLineItemCode());
                IndentInventoryList refItem = items.get(0);
                PurchaseOrderIndentRef ref = new PurchaseOrderIndentRef();
                ref.setIndentLineItemCode(refItem.getLineItemCode());
                ref.setIndentNo(refItem.getIndentInventory().getIndentId());
                ref.setPoLine(line);
                line.getIndentRefs().add(ref);
                processedCodes.add(ref.getIndentLineItemCode());
            }
            double rate = line.getRate() != null ? line.getRate() : 0;
            double disc = line.getDiscountPercent() != null ? line.getDiscountPercent() : 0;
            double gst = line.getGstPercent() != null ? line.getGstPercent() : 0;
            double discountedRate = rate - (rate * disc / 100);
            double netRate = discountedRate * newQty;
            double totalAmount = netRate + (netRate * gst / 100);
            line.setNetRate(netRate);
            line.setTotalAmount(totalAmount);
        } else {
            PurchaseOrderLine newLine = poBuilder.buildPoLine(po, req);
            po.getLines().add(newLine);
            if (newLine.getIndentRefs() != null) {
                newLine.getIndentRefs().forEach(r -> processedCodes.add(r.getIndentLineItemCode()));
            }
        }

        recalculateGrandTotal(po);
        PurchaseOrder saved = purchaseOrderRepo.save(po);

        for (String code : processedCodes) {
            indentStatusUpdater.markIndentLineAsPOCreated(code, saved.getPurchaseOrderId());
        }

        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(saved, saved.getStatus(), saved.getStatus(), username,
                processedCodes.size() + " indent(s) added/clubbed to PO by " + username, null);

        return getPurchaseOrderWithInit(saved.getPurchaseOrderId());
    }

    /**
     * Adds multiple new line items to an existing PO in a single transaction.
     * Each line is validated and its linked indent line is marked as PO CREATED.
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder addLineItems(String poId, List<CreatePoLineRequest> reqs) throws Exception {
        if (reqs == null || reqs.isEmpty()) {
            throw new Exception("At least one line item is required.");
        }
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new Exception("Purchase Order not found: " + poId));

        validator.validateAddLineToPO(po);
        validator.validateIndentLineItems(reqs);

        List<String> processedIndentCodes = new ArrayList<>();

        for (CreatePoLineRequest req : reqs) {
            // Club into existing line if PO already has a line for this product
            Optional<PurchaseOrderLine> existing = po.getLines().stream()
                    .filter(l -> l.getProduct() != null
                            && req.getProductId() != null
                            && l.getProduct().getProductId().equals(req.getProductId()))
                    .findFirst();

            if (existing.isPresent()) {
                PurchaseOrderLine line = existing.get();
                if (req.getIndentRefs() == null || req.getIndentRefs().isEmpty()) {
                    throw new Exception("Indent refs are required when clubbing into an existing PO line.");
                }
                double newQty = (line.getQuantity() != null ? line.getQuantity() : 0)
                        + (req.getQuantity() != null ? req.getQuantity() : 0);
                line.setQuantity(newQty);

                if (req.getIndentRefs() != null) {
                    for (IndentLineRefRequest refReq : req.getIndentRefs()) {
                        List<IndentInventoryList> items =
                                indentInventoryListRepo.findByLineItemCode(refReq.getIndentLineItemCode());
                        if (items.isEmpty())
                            throw new Exception("Indent line item not found: " + refReq.getIndentLineItemCode());
                        IndentInventoryList refItem = items.get(0);
                        PurchaseOrderIndentRef ref = new PurchaseOrderIndentRef();
                        ref.setIndentLineItemCode(refItem.getLineItemCode());
                        ref.setIndentNo(refItem.getIndentInventory().getIndentId());
                        ref.setPoLine(line);
                        line.getIndentRefs().add(ref);
                        processedIndentCodes.add(ref.getIndentLineItemCode());
                    }
                }

                // Recalculate totals using existing rate/discount/gst
                double rate = line.getRate() != null ? line.getRate() : 0;
                double disc = line.getDiscountPercent() != null ? line.getDiscountPercent() : 0;
                double gst = line.getGstPercent() != null ? line.getGstPercent() : 0;
                double discountedRate = rate - (rate * disc / 100);
                double netRate = discountedRate * newQty;
                double totalAmount = netRate + (netRate * gst / 100);
                line.setNetRate(netRate);
                line.setTotalAmount(totalAmount);
            } else {
                // New product — create a fresh PO line
                PurchaseOrderLine newLine = poBuilder.buildPoLine(po, req);
                po.getLines().add(newLine);
                if (newLine.getIndentRefs() != null) {
                    newLine.getIndentRefs().forEach(r -> processedIndentCodes.add(r.getIndentLineItemCode()));
                }
            }
        }

        recalculateGrandTotal(po);
        PurchaseOrder saved = purchaseOrderRepo.save(po);

        // Mark all processed indent lines as PO_CREATED
        for (String indentCode : processedIndentCodes) {
            indentStatusUpdater.markIndentLineAsPOCreated(indentCode, saved.getPurchaseOrderId());
        }

        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(saved, saved.getStatus(), saved.getStatus(), username,
                processedIndentCodes.size() + " indent(s) added/clubbed to PO by " + username, null);

        return getPurchaseOrderWithInit(saved.getPurchaseOrderId());
    }

    /**
     * Removes an open line item from an existing PO.
     * The line's linked indent item must be in PO CREATED status (no inward started).
     * Cannot remove the last remaining line on a PO.
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder removeLineItem(String poId, Long lineId) throws Exception {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new Exception("Purchase Order not found: " + poId));

        // Block terminal-status POs
        validator.validateAddLineToPO(po); // reuses terminal-status check

        // Find the target line
        PurchaseOrderLine lineToRemove = po.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new Exception("PO line not found: " + lineId));

        // Must not be the last line
        if (po.getLines().size() <= 1) {
            throw new Exception("Cannot remove the last line item from a Purchase Order.");
        }

        // Validate indent line statuses allow removal
        validator.validateLineRemovable(lineToRemove);

        // Revert indent statuses BEFORE removing from set (so refs are still accessible)
        for (PurchaseOrderIndentRef ref : lineToRemove.getIndentRefs()) {
            indentStatusUpdater.revertIndentLineToNew(ref.getIndentLineItemCode(), poId);
        }

        // Remove from PO — orphanRemoval will delete from DB on save
        po.getLines().remove(lineToRemove);

        recalculateGrandTotal(po);

        PurchaseOrder saved = purchaseOrderRepo.save(po);

        // Re-evaluate PO header status (may flip PARTIAL→NEW if this was the last active line)
        poCompletionEvaluator.evaluate(saved);

        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(saved, saved.getStatus(), saved.getStatus(), username,
                "Line item (ID: " + lineId + ") removed from PO by " + username, null);

        return getPurchaseOrderWithInit(saved.getPurchaseOrderId());
    }

    /**
     * Updates only the tolerance % of a single PO line. Allowed in NEW and PARTIAL status;
     * blocked once the PO reaches a terminal status (CANCELLED, COMPLETED, SHORT CLOSED).
     */
    @Transactional
    public PurchaseOrder updateLineTolerance(String poId, Long lineId, Double tolerancePercent) throws Exception {
        PurchaseOrder po = purchaseOrderRepo.findById(poId)
                .orElseThrow(() -> new Exception("Purchase Order not found: " + poId));

        validator.validateAddLineToPO(po); // reuses terminal-status check

        PurchaseOrderLine line = po.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new Exception("PO line not found: " + lineId));

        line.setTolerancePercent(tolerancePercent != null ? tolerancePercent : 0.0);

        PurchaseOrder saved = purchaseOrderRepo.save(po);
        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(saved, saved.getStatus(), saved.getStatus(), username,
                "Tolerance % updated on line (ID: " + lineId + ") by " + username, null);
        String activityUser = resolveCurrentUser();
        activityLogService.record("UPDATED", "PURCHASE_ORDER", saved.getPurchaseOrderId(),
                "Tolerance % updated on PO " + saved.getPurchaseOrderId() + " by " + activityUser, activityUser);
        return getPurchaseOrderWithInit(saved.getPurchaseOrderId());
    }

    /** Returns an integer rank for indent line item status (higher = more advanced). */
    private int statusRank(String status) {
        if (status == null) return 0;
        switch (status) {
            case "NEW":             return 1;
            case "PO CREATED":      return 2;
            case "CANCELLED":       return 3;
            case "SHORT CLOSED":    return 4;
            case "INWARD PARTIAL":  return 5;
            case "INWARD COMPLETE": return 6;
            default:                return 1;
        }
    }

    /** Recomputes PO grand total = sum of line totals + freight + custom charges. */
    private void recalculateGrandTotal(PurchaseOrder po) {
        double linesTotal = po.getLines().stream()
                .mapToDouble(l -> l.getTotalAmount() != null ? l.getTotalAmount() : 0.0)
                .sum();
        double freightTotal = po.getTotalFreightCharges() != null ? po.getTotalFreightCharges() : 0.0;
        double customTotal = po.getCustomCharges().stream()
                .mapToDouble(c -> c.getTotalChargeAmount() != null ? c.getTotalChargeAmount() : 0.0)
                .sum();
        // PO Discount is a flat, post-tax PO-level deduction — not baked into any line's
        // totalAmount, so it must be subtracted here explicitly.
        double poDiscount = po.getPoDiscount() != null ? po.getPoDiscount() : 0.0;
        po.setGrandTotal(linesTotal + freightTotal + customTotal - poDiscount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPurchaseOrderById(String id) throws Exception {
        poLifecycleManager.cancelIfAllowed(id);
    }

    @Transactional
    public PurchaseOrder shortClosePurchaseOrder(ShortClosePoRequest request) throws Exception {
        if (request.getPurchaseOrderNo() == null)
            throw new IllegalArgumentException("Purchase Order Number cannot be null");
        poLifecycleManager.shortClosePo(request);
        return purchaseOrderRepo.findByIdWithDetails(request.getPurchaseOrderNo()).get();
    }

    private String resolveCurrentUser() {
        try { return userDetailsService.getCurrentUser().getUsername(); }
        catch (Exception e) { return "System"; }
    }

    private void validatePoDateBackdating(Date poDate) throws Exception {
        if (poDate == null) return;
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Calendar poCal = Calendar.getInstance();
        poCal.setTime(poDate);
        poCal.set(Calendar.HOUR_OF_DAY, 0);
        poCal.set(Calendar.MINUTE, 0);
        poCal.set(Calendar.SECOND, 0);
        poCal.set(Calendar.MILLISECOND, 0);
        if (poCal.before(todayCal)) {
            boolean isAdmin = userDetailsService.getCurrentUser().getRoles().stream()
                    .anyMatch(r -> r.toLowerCase().contains(RoleConstants.ADMIN));
            if (!isAdmin) {
                throw new IllegalArgumentException("Only admin users can create backdated Purchase Orders.");
            }
        }
    }

    private String buildPoCreationMessage(CreatePoRequest request, String username) {
        String indentDetails = request.getLineItems().stream()
                .flatMap(line ->
                        line.getIndentRefs().stream()
                                .map(ref ->
                                        ref.getIndentLineItemCode() +
                                                " (Qty: " + line.getQuantity() + ")"
                                )
                )
                .collect(Collectors.joining(", "));

        return "Purchase Order created by user " + username + ". Indent line items: " + indentDetails;
    }

    private List<HistoryRelationInput> buildPoCreationRelations(CreatePoRequest request) {
        return request.getLineItems().stream()
                .flatMap(line ->
                        line.getIndentRefs().stream()
                                .map(ref -> extractIndentId(ref.getIndentLineItemCode()))
                )
                .distinct()
                .map(indentId ->
                        new HistoryRelationInput(
                                HistoryRelationType.INDENT,
                                "",
                                indentId
                        )
                )
                .collect(Collectors.toList());
    }

    private String extractIndentId(String indentLineItemCode) {
        if (indentLineItemCode == null || !indentLineItemCode.contains("/")) {
            throw new IllegalArgumentException(
                    "Invalid indent line item code: " + indentLineItemCode
            );
        }
        return indentLineItemCode.substring(0, indentLineItemCode.indexOf('/'));
    }

    @Transactional(readOnly = true)
    public Map<String, DashboardChartDTO> getCurrentPODashboards() {

        List<String> statuses = Arrays.asList(
                POStatusConstants.STATUS_NEW,      // Zero Inward
                POStatusConstants.STATUS_PARTIAL   // Partial
        );

        List<StatusGroupCountDTO> rows = purchaseOrderRepo.fetchCurrentPOStatusCounts(statuses);

        Map<String, Map<String, Long>> grouped = new HashMap<>();

        for (StatusGroupCountDTO row : rows) {
            grouped
                    .computeIfAbsent(row.getStatus(), k -> new HashMap<>())
                    .merge(row.getGroupKey(), row.getCount(), Long::sum);
        }

        Map<String, DashboardChartDTO> dashboards = new HashMap<>();

        for (String status : statuses) {
            Map<String, Long> firmMap =
                    grouped.getOrDefault(status, new HashMap<>());

            List<TenantCountDTO> firmCounts = new ArrayList<>();
            long total = 0;

            for (Map.Entry<String, Long> e : firmMap.entrySet()) {
                firmCounts.add(new TenantCountDTO(e.getKey(), e.getValue()));
                total += e.getValue();
            }

            dashboards.put(status,
                    new DashboardChartDTO(total, firmCounts));
        }

        return dashboards;
    }

    public void streamPurchaseOrderExcel(
            FilterDataList filterDataList,
            OutputStream os) {

        SXSSFWorkbook workbook = null;

        try {
            workbook = new SXSSFWorkbook(100);
            Sheet sheet = workbook.createSheet("Purchase Orders");

            int rowNum = 0;

            // ===== Header =====
            Row header = sheet.createRow(rowNum++);
            String[] headers = {
                    "PO Number",
                    "PO Date",
                    "PO Status",
                    "Project",

                    "Supplier",
                    "Firm",

                    "Product",
                    "Brand",
                    "Grade",
                    "Specification",
                    "Quantity",
                    "Received Quantity",
                    "Remaining Quantity",
                    "Rate",
                    "GST %",
                    "Net Rate",
                    "Total Amount",

                    "Indent No",
                    "Indent Line Item Code"
            };

            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Specification<PurchaseOrder> spec =
                    PurchaseOrderSpecification.getSpecification(filterDataList);

            int page = 0;
            int size = 200;
            Page<PurchaseOrder> poPage;

            do {
                Pageable pageable = PageRequest.of(page, size);
                poPage = purchaseOrderRepo.findAll(spec, pageable);

                List<String> poIds = poPage.getContent().stream()
                        .map(PurchaseOrder::getPurchaseOrderId)
                        .collect(Collectors.toList());

                if (!poIds.isEmpty()) {

                    List<PurchaseOrder> purchaseOrders =
                            purchaseOrderRepo.findWithDetailsByIdIn(poIds);

                    // Collect all indent line item codes in this page for batch fetch
                    List<String> lineItemCodes = purchaseOrders.stream()
                            .flatMap(po -> po.getLines().stream())
                            .flatMap(line -> line.getIndentRefs() == null
                                    ? java.util.stream.Stream.empty()
                                    : line.getIndentRefs().stream())
                            .map(PurchaseOrderIndentRef::getIndentLineItemCode)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    Map<String, IndentInventoryList> indentLineMap = new HashMap<>();
                    if (!lineItemCodes.isEmpty()) {
                        indentInventoryListRepo.findByLineItemCodeIn(lineItemCodes)
                                .forEach(il -> indentLineMap.put(il.getLineItemCode(), il));
                    }

                    // ===== Flatten PO → Lines =====
                    for (PurchaseOrder po : purchaseOrders) {

                        String supplierName =
                                po.getSupplier() != null
                                        ? po.getSupplier().getName()
                                        : "";

                        String firmName =
                                po.getFirm() != null
                                        ? po.getFirm().getFirmName()
                                        : "";

                        for (PurchaseOrderLine line : po.getLines()) {

                            if (line.getIndentRefs() == null
                                    || line.getIndentRefs().isEmpty()) {

                                // Still export line even if no indent
                                Row row = sheet.createRow(rowNum++);
                                writePoRow(row, po, supplierName, firmName,
                                        line, "", "", null);
                            } else {
                                for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                                    Row row = sheet.createRow(rowNum++);
                                    writePoRow(
                                            row,
                                            po,
                                            supplierName,
                                            firmName,
                                            line,
                                            extractIndentId(ref.getIndentLineItemCode()),
                                            ref.getIndentLineItemCode(),
                                            indentLineMap.get(ref.getIndentLineItemCode())
                                    );
                                }
                            }
                        }
                    }
                }

                page++;
            } while (!poPage.isLast());

            workbook.write(os);
            os.flush();

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass())
                    .error("PO Excel export error", e);
        } finally {
            if (workbook != null) {
                workbook.dispose();
            }
        }
    }

    private void writePoRow(
            Row row,
            PurchaseOrder po,
            String supplierName,
            String firmName,
            PurchaseOrderLine line,
            String indentNo,
            String indentLineItemCode,
            IndentInventoryList indentLineItem) {

        int col = 0;

        row.createCell(col++).setCellValue(po.getPurchaseOrderId());
        row.createCell(col++).setCellValue(
                po.getPoDate() != null ? po.getPoDate().toString() : ""
        );
        row.createCell(col++).setCellValue(safeExcel(po.getStatus()));
        row.createCell(col++).setCellValue(safeExcel(po.getProjectName()));

        row.createCell(col++).setCellValue(safeExcel(supplierName));
        row.createCell(col++).setCellValue(safeExcel(firmName));

        row.createCell(col++).setCellValue(
                safeExcel(line.getProduct() != null ? line.getProduct().getProductName() : "")
        );
        row.createCell(col++).setCellValue(safeExcel(line.getBrand()));
        row.createCell(col++).setCellValue(safeExcel(line.getGrade()));
        row.createCell(col++).setCellValue(safeExcel(line.getSpecification()));

        row.createCell(col++).setCellValue(
                line.getQuantity() != null ? line.getQuantity() : 0.0
        );
        row.createCell(col++).setCellValue(
                indentLineItem != null && indentLineItem.getQuantityReceived() != null
                        ? indentLineItem.getQuantityReceived() : 0.0
        );
        row.createCell(col++).setCellValue(
                indentLineItem != null && indentLineItem.getQuantityPending() != null
                        ? indentLineItem.getQuantityPending() : 0.0
        );

        row.createCell(col++).setCellValue(
                line.getRate() != null ? line.getRate() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getGstPercent() != null ? line.getGstPercent() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getNetRate() != null ? line.getNetRate() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getTotalAmount() != null ? line.getTotalAmount() : 0.0
        );

        row.createCell(col++).setCellValue(safeExcel(indentNo));
        row.createCell(col++).setCellValue(safeExcel(indentLineItemCode));
    }

    // ── Overdue PO lines (dashboard widget) ─────────────────────────────────

    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager entityManager;

    /**
     * Returns a paginated list of open PO line items that have exceeded their
     * effective lead time (product-level → category-level → not tracked).
     * Result ordered by daysOverdue DESC (most overdue first).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOverdueLines(int page, int size) {
        String terminalIn = "'" + String.join("','", POStatusConstants.getTerminalStatuses()) + "'";

        String baseSelect =
            "SELECT po.purchase_order_id, DATE_FORMAT(po.po_date,'%d-%m-%Y') AS po_date, " +
            "  po.project_name, " +
            "  COALESCE(s.name,'') AS supplier_name, " +
            "  p.product_name, p.product_code, p.measurementUnit, " +
            "  COALESCE(p.lead_time_days, cat.lead_time_days) AS lead_time_days, " +
            "  (DATEDIFF(CURDATE(), po.po_date) - COALESCE(p.lead_time_days, cat.lead_time_days)) AS days_overdue ";

        String baseFrom =
            "FROM purchase_order po " +
            "JOIN purchase_order_line pol ON pol.po_id = po.purchase_order_id AND pol.is_deleted = 0 " +
            "JOIN Product p ON p.productId = pol.product_id AND p.is_deleted = 0 " +
            "LEFT JOIN Category cat ON cat.categoryId = p.categoryId AND cat.is_deleted = 0 " +
            "LEFT JOIN contacts s ON s.contactId = po.supplier_id AND s.contacttype = 'supplier' AND s.is_deleted = 0 ";

        String baseWhere =
            "WHERE po.is_deleted = 0 " +
            "AND pol.is_deleted = 0 " +
            "AND po.status NOT IN (" + terminalIn + ") " +
            "AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL " +
            "AND DATEDIFF(CURDATE(), po.po_date) > COALESCE(p.lead_time_days, cat.lead_time_days) ";

        String orderBy = "ORDER BY days_overdue DESC ";

        // Total count
        Number totalNum = (Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) " + baseFrom + baseWhere
        ).getSingleResult();
        long total = totalNum != null ? totalNum.longValue() : 0L;

        // Data page
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
            baseSelect + baseFrom + baseWhere + orderBy + "LIMIT :lim OFFSET :off"
        )
        .setParameter("lim", size)
        .setParameter("off", page * size)
        .getResultList();

        List<OverduePOLineDTO> content = new ArrayList<>();
        for (Object[] r : rows) {
            OverduePOLineDTO dto = new OverduePOLineDTO();
            dto.setPurchaseOrderId(r[0] != null ? r[0].toString() : null);
            dto.setPoDate(r[1] != null ? r[1].toString() : null);
            dto.setProjectName(r[2] != null ? r[2].toString() : null);
            dto.setSupplierName(r[3] != null ? r[3].toString() : null);
            dto.setProductName(r[4] != null ? r[4].toString() : null);
            dto.setProductCode(r[5] != null ? r[5].toString() : null);
            dto.setMeasurementUnit(r[6] != null ? r[6].toString() : null);
            dto.setLeadTimeDays(r[7] != null ? ((Number) r[7]).intValue() : null);
            dto.setDaysOverdue(r[8] != null ? ((Number) r[8]).intValue() : null);
            content.add(dto);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public long getOverduePOCount() {
        String terminalIn = "'" + String.join("','", POStatusConstants.getTerminalStatuses()) + "'";
        String baseFrom =
            "FROM purchase_order po " +
            "JOIN purchase_order_line pol ON pol.po_id = po.purchase_order_id AND pol.is_deleted = 0 " +
            "JOIN Product p ON p.productId = pol.product_id AND p.is_deleted = 0 " +
            "LEFT JOIN Category cat ON cat.categoryId = p.categoryId AND cat.is_deleted = 0 ";
        String baseWhere =
            "WHERE po.is_deleted = 0 " +
            "AND pol.is_deleted = 0 " +
            "AND po.status NOT IN (" + terminalIn + ") " +
            "AND COALESCE(p.lead_time_days, cat.lead_time_days) IS NOT NULL " +
            "AND DATEDIFF(CURDATE(), po.po_date) > COALESCE(p.lead_time_days, cat.lead_time_days) ";
        Number result = (Number) entityManager.createNativeQuery(
            "SELECT COUNT(DISTINCT po.purchase_order_id) " + baseFrom + baseWhere
        ).getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    private String safeExcel(String value) {
        if (value == null) return "";
        if (value.startsWith("=") || value.startsWith("+")
                || value.startsWith("-") || value.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }

}