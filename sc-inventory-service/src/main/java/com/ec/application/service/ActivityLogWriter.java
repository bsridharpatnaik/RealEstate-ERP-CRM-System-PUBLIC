package com.ec.application.service;

import com.ec.application.model.ActivityLog;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogWriter.class);

    private final ActivityLogRepository activityLogRepository;

    /**
     * Runs in async thread. Tenant is passed explicitly because ThreadLocal is
     * poisoned to master schema by the time @Async task is submitted
     * (ProductService is @UseDefaultTenant at class level).
     */
    @Async("processExecutor")
    public void saveAsync(ActivityLog entry, String tenant) {
        try {
            ThreadLocalStorage.setTenantName(tenant);
            activityLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save activity log [{} {} {}]: {}",
                    entry.getAction(), entry.getEntityType(), entry.getEntityId(), e.getMessage());
        }
    }
}
