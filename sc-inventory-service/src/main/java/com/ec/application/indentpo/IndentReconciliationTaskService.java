package com.ec.application.indentpo;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IndentReconciliationTaskService {

    private final IndentReconciliationTaskRepo repo;
    private final TenantService tenantService;

    @Transactional
    public void enqueue(String lineItemCode) {

        IndentReconciliationTask task = new IndentReconciliationTask();
        task.setTenantSchema(ThreadLocalStorage.getTenantName()); // CURRENT tenant
        task.setLineItemCode(lineItemCode);
        task.setStatus("PENDING");
        task.setRetryCount(0);

        repo.save(task);
    }
}
