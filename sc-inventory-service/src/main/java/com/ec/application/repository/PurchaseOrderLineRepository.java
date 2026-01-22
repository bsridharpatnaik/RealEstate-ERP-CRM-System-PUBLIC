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
            "SELECT new com.ec.application.data.PreviousPurchaseRateDTO(" +
                    " po.purchaseOrderId, po.poDate, s.name, pol.rate) " +
                    "FROM PurchaseOrderLine pol " +
                    "JOIN pol.purchaseOrder po " +
                    "JOIN po.supplier s " +
                    "WHERE pol.product.productId = :productId " +
                    "AND po.status <> :cancelledStatus"
    )
    List<PreviousPurchaseRateDTO> findPreviousRates(@Param("productId") Long productId, @Param("cancelledStatus") String cancelledStatus, Pageable pageable);
}
