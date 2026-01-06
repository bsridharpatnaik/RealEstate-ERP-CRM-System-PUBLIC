package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.data.ConsolidatedIndentLineDTO;
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
public class ConsolidatedIndentAggregationService {

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
        targetSchemas = schemaConfig.getSchemaList().stream()
                .filter(s -> !s.equalsIgnoreCase(masterSchema))
                .collect(Collectors.toList());
    }

    public Map<String, List<ConsolidatedIndentLineDTO>>
    fetchGroupedByCategory() {

        List<ConsolidatedIndentLineDTO> allLines = new ArrayList<>();

        for (Map.Entry<String, String> entry : schemaConfig.getSchemaMap().entrySet()) {

            String tenantSchemaCode = entry.getKey();     // SC001
            String schemaName = tenantService.entry.getValue();         // smartcityv2

            ThreadLocalStorage.setTenantName(schemaName);
            try {
                List<ConsolidatedIndentLineDTO> tenantLines = indentInventoryRepo.fetchConsolidatedIndentLines();
                tenantLines.forEach(dto -> {
                    dto.setTenantSchemaCode(tenantSchemaCode);
                    dto.setTenantName(schemaName); // or friendly name if you have one
                });
                allLines.addAll(tenantLines);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        }
        return allLines.stream().collect(Collectors.groupingBy(ConsolidatedIndentLineDTO::getCategoryName, LinkedHashMap::new, Collectors.toList()));
    }
}