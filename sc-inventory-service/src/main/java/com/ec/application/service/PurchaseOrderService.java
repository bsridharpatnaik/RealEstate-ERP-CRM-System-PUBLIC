package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.PurchaseOrderSpecification;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.IndentLineItemStatusConstants;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.data.*;
import com.ec.application.model.*;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryListRepo;
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.PurchaseOrderRepo;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
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

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePoRequest request) throws Exception {
        validator.validateIndentLineItems(request.getLineItems());
        PurchaseOrder po = poBuilder.buildPurchaseOrder(request);
        PurchaseOrder savedPO = purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(savedPO.getLines());
        return savedPO;
    }

    @Transactional(readOnly = true)
    public ReturnPurchaseOrderData fetchIndentInventory(FilterDataList filterDataList, Pageable pageable) throws ParseException {

        ReturnIndentInventoryData returnData = new ReturnIndentInventoryData();
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.getSpecification(filterDataList);

        Page<PurchaseOrder> page = (spec != null) ? purchaseOrderRepo.findAll(spec, pageable) : purchaseOrderRepo.findAll(pageable);
        // Enrich ONCE for UI
        indentInventoryUiEnricher.enrich(page.getContent());
        returnData.setIndentInventories(page);
        returnData.setIiDropdown(populateDropdownService.fetchData("indent"));
        return returnData;
    }
}