package com.ec.application.service;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.constants.InwardActionType;
import com.ec.application.data.IndentInwardSyncDTO;
import com.ec.application.model.InwardSyncFailure;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InwardSyncFailureRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class InwardSyncFailureService {


    private final InwardSyncFailureRepo failureRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    @UseDefaultTenant
    public void recordFailure(IndentInwardSyncDTO dto, Exception ex) throws JsonProcessingException {
        try {
            if (failureRepo.existsByTenantSchemaAndInwardIdAndActionTypeAndStatus(dto.getTenantSchema(), dto.getInwardId(), dto.getActionType(), "PENDING")) {
                return;
            }
            InwardSyncFailure failure = new InwardSyncFailure();
            failure.setTenantSchema(dto.getTenantSchema());
            failure.setInwardId(dto.getInwardId());
            failure.setActionType(dto.getActionType());
            failure.setPayloadJson(objectMapper.writeValueAsString(dto));
            failure.setStatus("PENDING");
            failure.setRetryCount(0);
            failure.setLastError(ex.getMessage());
            failureRepo.save(failure);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
