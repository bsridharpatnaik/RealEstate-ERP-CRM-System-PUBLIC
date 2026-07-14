package com.ec.application.repository;

import com.ec.application.data.PreviousPurchaseRateDTO;
import com.ec.application.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderLineRepository
        extends JpaRepository<PurchaseOrderLine, Long> {

    @Query(
            "SELECT po.purchaseOrderId, po.poDate, s.name, pol.quantity, pol.rate, " +
                    "pol.discountPercent, pol.gstPercent, pol.netRate, s.contactId " +
                    "FROM PurchaseOrderLine pol " +
                    "JOIN pol.purchaseOrder po " +
                    "JOIN po.supplier s " +
                    "WHERE pol.product.productId = :productId " +
                    "AND po.status <> :cancelledStatus " +
                    "ORDER BY CAST(SUBSTRING(po.purchaseOrderId, LOCATE('-', po.purchaseOrderId) + 1) AS integer) DESC"
    )
    List<Object[]> findPreviousRates(@Param("productId") Long productId, @Param("cancelledStatus") String cancelledStatus);

    @Query(
            value = "SELECT product_id, computed_net_rate FROM (" +
                    "  SELECT pol.product_id," +
                    "         pol.rate - (pol.rate * COALESCE(pol.discountPercent, 0) / 100.0) AS computed_net_rate," +
                    "         ROW_NUMBER() OVER (PARTITION BY pol.product_id ORDER BY po.po_date DESC, pol.id DESC) AS rn" +
                    "  FROM purchase_order_line pol" +
                    "  INNER JOIN purchase_order po ON pol.po_id = po.purchase_order_id" +
                    "  WHERE po.is_deleted = 0 AND po.status <> 'Cancelled' AND pol.is_deleted = 0" +
                    ") ranked WHERE rn = 1",
            nativeQuery = true
    )
    List<Object[]> findLatestNetRatePerProduct();

    boolean existsByProduct_ProductIdAndBillingUnitAndIsDeletedFalse(
            Long productId, String billingUnit);

    @Query(
            "SELECT pol FROM PurchaseOrderLine pol " +
                    "JOIN FETCH pol.product " +
                    "JOIN pol.purchaseOrder po " +
                    "WHERE po.purchaseOrderId = :poNumber " +
                    "AND po.isDeleted = false"
    )
    List<PurchaseOrderLine> findLinesByPoNumber(@Param("poNumber") String poNumber);
}
