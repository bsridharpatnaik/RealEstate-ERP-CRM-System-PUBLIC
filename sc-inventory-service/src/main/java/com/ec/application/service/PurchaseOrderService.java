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

    List<String> poEligibleStatuses = Arrays.asList(IndentStatusConstants.STATUS_APPROVED, IndentStatusConstants.STATUS_PO_PARTIAL);

    public Map<String, List<ConsolidatedIndentLineDTO>> fetchGroupedByCategory() {
        List<ConsolidatedIndentLineDTO> allLines = new ArrayList<>();
        for (Map.Entry<String, String> entry : schemaConfig.getSchemaMap().entrySet()) {
            String tenantSchemaCode = entry.getValue();
            String schemaNameUpdated = entry.getKey();
            ThreadLocalStorage.setTenantName(schemaNameUpdated);
            try {
                List<IndentInventory> inventoryList = indentInventoryRepo.findByIndentStatusIn(poEligibleStatuses);
                List<ConsolidatedIndentLineDTO> result = flatten(inventoryList, entry.getKey(), tenantSchemaCode);
                allLines.addAll(result);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        }
        return allLines.stream().collect(Collectors.groupingBy(ConsolidatedIndentLineDTO::getCategoryName, LinkedHashMap::new, Collectors.toList()));
    }

    List<ConsolidatedIndentLineDTO> flatten(List<IndentInventory> indents, String tenantName, String tenantSchemaCode) {
        List<ConsolidatedIndentLineDTO> result = new ArrayList<>();

        for (IndentInventory indent : indents) {
            for (IndentInventoryList line : indent.getInventoryList()) {

                Product p = line.getProduct();
                Category c = p.getCategory();

                ConsolidatedIndentLineDTO dto =
                        new ConsolidatedIndentLineDTO(
                                tenantService.removePrefixForSuncity(tenantName),
                                tenantSchemaCode,
                                indent.getIndentDate(),
                                indent.getIndentId(),
                                line.getLineItemCode(),
                                c.getCategoryName(),
                                p.getProductId(),
                                p.getProductName(),
                                p.getMeasurementUnit(),
                                line.getQuantity(),
                                line.getSpecification(),
                                line.getRemarks(),
                                line.getLineItemStatus()
                        );

                result.add(dto);
            }
        }
        return result;
    }
}