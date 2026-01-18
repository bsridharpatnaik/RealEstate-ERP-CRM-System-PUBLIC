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
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.PurchaseOrderRepo;
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

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePoRequest request) throws Exception {
        validator.validateIndentLineItems(request.getLineItems());
        PurchaseOrder po = poBuilder.buildPurchaseOrder(request);
        PurchaseOrder savedPO = purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(savedPO, POIndentUpdateAction.CREATE_PO);
        return savedPO;
    }

    @Transactional(readOnly = true)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(FilterDataList filterDataList, Pageable pageable) throws ParseException {
        ReturnPurchaseOrderData returnData = new ReturnPurchaseOrderData();
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.getSpecification(filterDataList);
        Page<PurchaseOrder> page = (spec != null) ? purchaseOrderRepo.findAll(spec, pageable) : purchaseOrderRepo.findAll(pageable);

        // Initialize all lazy-loaded associations
        initializeLazyAssociations(page.getContent());
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
    public PurchaseOrder getPurchaseOrderWithInit(String id) {
        PurchaseOrder po = purchaseOrderRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Purchase Order not found with ID: " + id));
        // Initialize supplier & firm (for JSON)
        if (po.getSupplier() != null) {
            po.getSupplier().getName();
        }
        if (po.getFirm() != null) {
            po.getFirm().getFirmName();
        }
        // Initialize lines + indentRefs + product
        for (PurchaseOrderLine line : po.getLines()) {
            // force init of indentRefs
            line.getIndentRefs().size();

            // force init of product if serialized
            if (line.getProduct() != null) {
                line.getProduct().getProductName();
            }
        }
        return po;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPurchaseOrderById(String id) {
        poLifecycleManager.cancelIfAllowed(id);
    }

    @Transactional
    public PurchaseOrder shortClosePurchaseOrder(ShortClosePoRequest request) {
        if(request.getPurchaseOrderNo() == null)
            throw new IllegalArgumentException("Purchase Order Number cannot be null");
        poLifecycleManager.shortClosePo(request);
        return purchaseOrderRepo.findByIdWithDetails(request.getPurchaseOrderNo()).get();
    }
}