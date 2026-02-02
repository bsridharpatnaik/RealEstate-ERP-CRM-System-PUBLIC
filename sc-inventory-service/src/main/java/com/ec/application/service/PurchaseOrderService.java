package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.PurchaseOrderSpecification;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POIndentUpdateAction;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.*;
import com.ec.application.enricher.PurchaseOrderUiEnricher;
import com.ec.application.indentpo.PurchaseOrderLifecycleManager;
import com.ec.application.model.*;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.util.PurchaseOrderPriceMasker;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePoRequest request) throws Exception {
        validator.validateIndentLineItems(request.getLineItems());
        PurchaseOrder po = poBuilder.buildPurchaseOrder(request);
        PurchaseOrder savedPO = purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(savedPO, POIndentUpdateAction.CREATE_PO);
        draftService.deleteDraftForUser("PO");
        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(savedPO, null, savedPO.getStatus(), username, buildPoCreationMessage(request, username));
        return savedPO;
    }

    @Transactional(readOnly = true)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(FilterDataList filterDataList, Pageable pageable) throws Exception {

        ReturnPurchaseOrderData returnData = new ReturnPurchaseOrderData();
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.getSpecification(filterDataList);
        Page<PurchaseOrder> page = (spec != null)
                ? purchaseOrderRepo.findAll(spec, pageable)
                : purchaseOrderRepo.findAll(pageable);

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
                    // Initialize product
                    if (line.getProduct() != null) {
                        Hibernate.initialize(line.getProduct());
                        String productName = line.getProduct().getProductName(); // Touch to load
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

        // MASK PRICE FIELDS
        purchaseOrderPriceMasker.mask(po);
        return po;
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
}