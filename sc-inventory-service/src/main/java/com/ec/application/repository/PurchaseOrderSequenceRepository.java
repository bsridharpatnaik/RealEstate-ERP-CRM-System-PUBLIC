package com.ec.application.repository;

import com.ec.application.model.PurchaseOrderSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PurchaseOrderSequenceRepository
        extends JpaRepository<PurchaseOrderSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PurchaseOrderSequence s where s.tenantCode = :tenantCode")
    Optional<PurchaseOrderSequence> findForUpdate(@Param("tenantCode") String tenantCode);
}