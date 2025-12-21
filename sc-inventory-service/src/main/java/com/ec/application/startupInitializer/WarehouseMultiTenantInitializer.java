package com.ec.application.startupInitializer;

import com.ec.application.model.Warehouse;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.WarehouseRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WarehouseMultiTenantInitializer implements ApplicationRunner {

    private static final String DEFAULT_WAREHOUSE = "Dead Stock Warehouse";

    @Value("${schemas.list}")
    private String schemas;

    @Autowired
    private WarehouseRepo warehouseRepo;

    private static final Logger log = LoggerFactory.getLogger(WarehouseMultiTenantInitializer.class);

    @Override
    public void run(ApplicationArguments args) {

        List<String> tenants = Arrays.stream(schemas.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String tenant : tenants) {
            try {
                ThreadLocalStorage.setTenantName(tenant);
                createDefaultWarehouseIfMissing(tenant);
            } catch (Exception ex) {
                log.error("Failed to initialize warehouse for tenant: {}", tenant, ex);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        }
    }

    @Transactional
    protected void createDefaultWarehouseIfMissing(String tenant) {

        if (!warehouseRepo.existsByWarehouseNameIgnoreCase(DEFAULT_WAREHOUSE)) {
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseName(DEFAULT_WAREHOUSE);
            warehouseRepo.save(warehouse);
            log.info("[{}] Default warehouse created", tenant);
        } else {
            log.info("[{}] Default warehouse already exists", tenant);
        }
    }
}
