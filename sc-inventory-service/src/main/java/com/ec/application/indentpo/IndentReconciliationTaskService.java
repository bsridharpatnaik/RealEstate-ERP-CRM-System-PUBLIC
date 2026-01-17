package com.ec.application.indentpo;

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
        task.setTenant(tenantService.fetchTenantFromHeader());
        task.setLineItemCode(lineItemCode);
        task.setStatus("PENDING");
        repo.save(task);
    }
}
