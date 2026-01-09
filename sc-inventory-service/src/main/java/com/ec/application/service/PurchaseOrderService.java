package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.data.ConsolidatedIndentLineDTO;
import com.ec.application.model.Category;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventoryList;
import com.ec.application.model.Product;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.IndentInventoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    @Autowired
    TenantService tenantService;

    @Autowired
    IndentInventoryRepo indentInventoryRepo;

    @Autowired
    private SchemaConfig schemaConfig;

    @Value("${master.schema}")
    private String masterSchema;

    private List<String> targetSchemas;

    @PostConstruct
    public void init() {
        targetSchemas = schemaConfig.getSchemaList().stream().filter(s -> !s.equalsIgnoreCase(masterSchema)).collect(Collectors.toList());
    }




}