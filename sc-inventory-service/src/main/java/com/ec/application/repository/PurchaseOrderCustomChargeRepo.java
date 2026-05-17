package com.ec.application.repository;

import com.ec.application.model.PurchaseOrderCustomCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderCustomChargeRepo extends JpaRepository<PurchaseOrderCustomCharge, Long> {

    @Modifying
    @Query("DELETE FROM PurchaseOrderCustomCharge c WHERE c.purchaseOrder.purchaseOrderId = :poId")
    void deleteByPurchaseOrderId(@Param("poId") String poId);
}
