package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.InwardActionType;
import com.ec.application.model.InwardSyncFailure;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InwardSyncFailureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class InwardSyncFailureService {


    private final InwardSyncFailureRepo failureRepo;

    @Transactional
    public void recordFailure(String tenantSchema, Long inwardId, InwardActionType actionType, Set<String> lineItemCodes, Exception ex) {
        try {
            boolean alreadyExists = failureRepo.existsByTenantSchemaAndInwardIdAndActionTypeAndStatus(tenantSchema, inwardId, actionType, "PENDING");
            if (alreadyExists) {
                return;
            }
            InwardSyncFailure failure = new InwardSyncFailure();
            failure.setTenantSchema(tenantSchema);
            failure.setInwardId(inwardId);
            failure.setActionType(actionType);
            failure.setLineItemCodes(String.join(",", lineItemCodes));
            failure.setStatus("PENDING");
            failure.setRetryCount(0);
            failure.setLastError(ex.getMessage());
            failureRepo.save(failure);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
