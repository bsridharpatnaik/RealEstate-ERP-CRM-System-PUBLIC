package com.ec.application.repository;

import com.ec.application.model.PurchaseOrderIndentRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderIndentRefRepository extends JpaRepository<PurchaseOrderIndentRef, Long> {

    /**
     * For all active PO lines belonging to non-cancelled / non-completed POs,
     * return the indent line item codes and their linked line items.
     *
     * @param excludedStatuses PO statuses that are considered "closed" (e.g. CANCELLED, SHORT CLOSE, COMPLETE INWARD)
     */
    @Query(
        "SELECT ref FROM PurchaseOrderIndentRef ref " +
        "JOIN ref.poLine pol " +
        "JOIN pol.purchaseOrder po " +
        "WHERE po.isDeleted = false " +
        "AND po.status NOT IN :excludedStatuses"
    )
    List<PurchaseOrderIndentRef> findAllForOpenPurchaseOrders(
            @Param("excludedStatuses") List<String> excludedStatuses);

    /** Fetch all indent refs whose parent PO is in the given ID list. */
    @Query(
        "SELECT ref FROM PurchaseOrderIndentRef ref " +
        "JOIN ref.poLine pol " +
        "JOIN pol.purchaseOrder po " +
        "WHERE po.purchaseOrderId IN :poIds"
    )
    List<PurchaseOrderIndentRef> findByPurchaseOrderIdIn(@Param("poIds") List<String> poIds);

    /** Return distinct indent IDs linked to a specific PO (for print-with-indents). */
    @Query(
        "SELECT DISTINCT ref.indentNo FROM PurchaseOrderIndentRef ref " +
        "JOIN ref.poLine pol " +
        "JOIN pol.purchaseOrder po " +
        "WHERE po.purchaseOrderId = :poId " +
        "AND ref.isDeleted = false"
    )
    List<String> findDistinctIndentNosByPoId(@Param("poId") String poId);
}
