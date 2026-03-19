package com.ec.application.repository;

import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.model.PurchaseOrderLine;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderLineRepository
        extends JpaRepository<PurchaseOrderLine, Long> {

    @Query(
            "SELECT po.purchaseOrderId, po.poDate, s.name, pol.quantity, pol.rate " +
                    "FROM PurchaseOrderLine pol " +
                    "JOIN pol.purchaseOrder po " +
                    "JOIN po.supplier s " +
                    "WHERE pol.product.productId = :productId " +
                    "AND po.status <> :cancelledStatus " +
                    "ORDER BY CAST(SUBSTRING(po.purchaseOrderId, LOCATE('-', po.purchaseOrderId) + 1) AS integer) DESC"
    )
    List<Object[]> findPreviousRates(@Param("productId") Long productId, @Param("cancelledStatus") String cancelledStatus, Pageable pageable);

    @Query(
            "SELECT pol FROM PurchaseOrderLine pol " +
                    "JOIN FETCH pol.product " +
                    "JOIN pol.purchaseOrder po " +
                    "WHERE po.purchaseOrderId = :poNumber " +
                    "AND po.isDeleted = false"
    )
    List<PurchaseOrderLine> findLinesByPoNumber(@Param("poNumber") String poNumber);
}
