package com.ec.application.repository;


import com.ec.application.model.IndentsForInwardView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IndentsForInwardViewRepository
        extends JpaRepository<IndentsForInwardView, String> {

    /**
     * 1️⃣ Fetch ONLY pending inward line items for a PO & tenant
     * Pending = ordered qty > already inwarded qty
     */
    @Query("SELECT v FROM IndentsForInwardView v WHERE v.purchaseOrderNumber =:poNumber AND v.tenant =:tenant AND v.quantity > v.totalInwardQuantity AND v.lineItemStatus IN :statuses ")
    List<IndentsForInwardView> findPendingLineItemsForPO(@Param("statuses") Collection<String> statuses, @Param("poNumber") String poNumber, @Param("tenant") String tenant);

    /**
     * 2️⃣ Dropdown – only POs having at least one pending inward item
     */
    @Query("SELECT DISTINCT v.purchaseOrderNumber, v.poDate, v.supplierName FROM IndentsForInwardView v WHERE v.tenant = :tenant AND v.quantity > v.totalInwardQuantity  AND v.lineItemStatus IN :statuses")
    List<Object[]> findPendingPoDropdown(@Param("statuses") Collection<String> statuses, @Param("tenant") String tenant);

    /**
     * 3️⃣ Optimistic locking + stale UI protection
     */
    @Query("SELECT v FROM IndentsForInwardView v WHERE v.lineItemCode = :lineItemCode AND v.lineItemStatus IN :statuses AND v.quantity > v.totalInwardQuantity AND v.tenant =:tenant")
    List<IndentsForInwardView> getLineItemForInward(@Param("lineItemCode") String lineItemCode, @Param("statuses") Collection<String> statuses, @Param("tenant") String tenant);

    @Query("SELECT v FROM IndentsForInwardView v WHERE v.lineItemStatus IN :statuses AND v.quantity > v.totalInwardQuantity AND v.tenant =:tenant")
    List<IndentsForInwardView> getLineItemsForInward(@Param("statuses") Collection<String> statuses, @Param("tenant") String tenant);

    @Query("SELECT v FROM IndentsForInwardView v WHERE v.lineItemCode = :lineItemCode AND v.tenant =:tenant")
    List<IndentsForInwardView> getLineItemDetails(@Param("lineItemCode") String lineItemCode, @Param("tenant") String tenant);

}
