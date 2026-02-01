package com.ec.application.repository;

import com.ec.application.model.PurchaseOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderStatusHistoryRepo extends JpaRepository<PurchaseOrderStatusHistory, Long> {
}