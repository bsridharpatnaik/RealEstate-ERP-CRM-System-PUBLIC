package com.ec.application.repository;

import com.ec.application.constants.InwardActionType;
import com.ec.application.model.InwardSyncFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InwardSyncFailureRepo extends JpaRepository<InwardSyncFailure, Long> {

    List<InwardSyncFailure> findTop20ByStatusOrderByCreationDateAsc(String status);

    boolean existsByTenantSchemaAndInwardIdAndActionTypeAndStatus(
            String tenantSchema,
            Long inwardId,
            InwardActionType actionType,
            String status
    );
}
