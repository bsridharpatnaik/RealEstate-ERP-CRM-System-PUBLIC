package com.ec.application.repository;

import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.data.TenantCountDTO;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PurchaseOrderStatusHistoryRepo extends JpaRepository<PurchaseOrderStatusHistory, Long> {
    List<PurchaseOrderStatusHistory> findByPurchaseOrderOrderByIdDesc(PurchaseOrder purchaseOrder);

    @Query("SELECT h FROM PurchaseOrderStatusHistory h WHERE h.purchaseOrder.id = :purchaseOrderId ORDER BY h.id DESC")
    List<PurchaseOrderStatusHistory> findByPurchaseOrderIdOrderByIdDesc(@Param("purchaseOrderId") String purchaseOrderId);

    @Query(
            "SELECT new com.ec.application.data.StatusGroupCountDTO(" +
                    "   posh.newStatus, " +
                    "   posh.purchaseOrder.firm.firmName, " +
                    "   COUNT(DISTINCT posh.purchaseOrder.purchaseOrderId)" +
                    ") " +
                    "FROM PurchaseOrderStatusHistory posh " +
                    "WHERE posh.newStatus IN :statuses " +
                    "AND posh.purchaseOrder.isDeleted = false " +
                    "AND posh.changedAt BETWEEN :startDate AND :endDate " +
                    "GROUP BY posh.newStatus, posh.purchaseOrder.firm.firmName"
    )
    List<StatusGroupCountDTO> fetchPODashboardData(@Param("statuses") List<String> statuses, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}