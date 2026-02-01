package com.ec.application.repository;

import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderStatusHistoryRepo extends JpaRepository<PurchaseOrderStatusHistory, Long> {
    List<PurchaseOrderStatusHistory> findByPurchaseOrderOrderByIdDesc(PurchaseOrder purchaseOrder);

    @Query("SELECT h FROM PurchaseOrderStatusHistory h WHERE h.purchaseOrder.id = :purchaseOrderId ORDER BY h.id DESC")
    List<PurchaseOrderStatusHistory> findByPurchaseOrderIdOrderByIdDesc(@Param("purchaseOrderId") String purchaseOrderId);
}