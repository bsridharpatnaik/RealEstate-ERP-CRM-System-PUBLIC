package com.ec.application.repository;

import com.ec.application.constants.InwardActionType;
import com.ec.application.model.InwardSyncFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.persistence.LockModeType;
import java.util.List;

public interface InwardSyncFailureRepo extends JpaRepository<InwardSyncFailure, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InwardSyncFailure> findTop20ByStatusOrderByCreationDateAsc(String status);

    boolean existsByTenantSchemaAndInwardIdAndActionTypeAndStatus(
            String tenantSchema,
            Long inwardId,
            InwardActionType actionType,
            String status
    );
}
