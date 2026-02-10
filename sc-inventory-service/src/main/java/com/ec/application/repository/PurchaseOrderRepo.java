package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.model.PurchaseOrder;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepo extends BaseRepository<PurchaseOrder, String> {
    @EntityGraph(attributePaths = {
            "supplier",
            "firm",
            "lines",
            "lines.product",
            "lines.indentRefs"
    })
    @Query("SELECT po FROM PurchaseOrder po WHERE po.purchaseOrderId = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") String id);

    @Query(
            "SELECT new com.ec.application.data.StatusGroupCountDTO(" +
                    "   po.status, " +
                    "   po.firm.firmName, " +
                    "   COUNT(po.purchaseOrderId)" +
                    ") " +
                    "FROM PurchaseOrder po " +
                    "WHERE po.status IN :statuses " +
                    "AND po.isDeleted = false " +
                    "GROUP BY po.status, po.firm.firmName"
    )
    List<StatusGroupCountDTO> fetchCurrentPOStatusCounts(@Param("statuses") List<String> statuses
    );


    @Query(
            value =
                    "SELECT " +
                            "  s.name AS supplier, " +
                            "  CASE " +
                            "    WHEN po.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 'GT_30_DAYS' " +
                            "    WHEN po.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 15 DAY) THEN 'GT_15_DAYS' " +
                            "    WHEN po.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 7 DAY)  THEN 'GT_7_DAYS'  " +
                            "    WHEN po.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 3 DAY)  THEN 'GT_3_DAYS'  " +
                            "  END AS bucket, " +
                            "  COUNT(*) AS cnt " +
                            "FROM purchase_order po " +
                            "JOIN contacts s ON s.contactId = po.supplier_id " +
                            "WHERE po.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 3 DAY) " +
                            "  AND po.status NOT IN (:terminalStatuses) " +
                            "  AND po.is_deleted = 0 " +
                            "GROUP BY s.name, bucket",
            nativeQuery = true
    )
    List<Object[]> fetchStalePOBucketData(@Param("terminalStatuses") List<String> terminalStatuses);

   // @EntityGraph(attributePaths = {})
        // no collections
    Page<PurchaseOrder> findAll(
            Specification<PurchaseOrder> spec,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "supplier",
            "firm",
            "lines",
            "lines.product",
            "lines.indentRefs"
    })
    @Query("select po from PurchaseOrder po where po.purchaseOrderId in :ids")
    List<PurchaseOrder> findWithDetailsByIdIn(
            @Param("ids") List<String> ids
    );
}

