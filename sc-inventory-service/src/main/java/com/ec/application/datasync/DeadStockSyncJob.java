package com.ec.application.datasync;

import com.ec.application.config.SchemaConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.DeadStockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class DeadStockSyncJob {

    @Autowired
    private DeadStockService deadStockService;

    @Autowired
    private SchemaConfig schemaConfig;

    @Value("${master.schema}")
    private String masterSchema;

    private List<String> targetSchemas;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final Logger log =
            LoggerFactory.getLogger(DeadStockSyncJob.class);

    @PostConstruct
    public void init() {
        targetSchemas = schemaConfig.getSchemaList().stream()
                .filter(s -> !s.equalsIgnoreCase(masterSchema))
                .collect(Collectors.toList());
    }

    public void syncAllTenants() {

        if (!running.compareAndSet(false, true)) {
            log.warn("Dead stock sync already running. Skipping.");
            return;
        }

        try {
            for (String tenant : targetSchemas) {
                log.info("Starting dead stock sync for tenant {}", tenant);

                ThreadLocalStorage.setTenantName(tenant);   // ✅ SET BEFORE TX
                deadStockService.syncTenantDeadStock(tenant);
            }
        } finally {
            ThreadLocalStorage.setTenantName(null);
            running.set(false);
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
