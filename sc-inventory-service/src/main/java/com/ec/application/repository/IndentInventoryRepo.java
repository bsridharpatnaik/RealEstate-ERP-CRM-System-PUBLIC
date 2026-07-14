package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ConsolidatedIndentLineDTO;
import com.ec.application.data.IndentForDropdownDTO;
import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.InwardInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndentInventoryRepo extends BaseRepository<IndentInventory, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    InwardInventory save(InwardInventory entity);

    @Query("SELECT DISTINCT i FROM IndentInventory i " +
            "LEFT JOIN FETCH i.inventoryList " +
            "LEFT JOIN FETCH i.fileInformations " +
            "WHERE i.indentId = :id")
    Optional<IndentInventory> findByIdWithDetails(@Param("id") String id);

    List<IndentInventory> findByIndentStatusIn(List<String> statuses);

    @Query(
            "SELECT new com.ec.application.data.StatusGroupCountDTO(" +
                    "   ii.indentStatus, " +
                    "   ii.tenant, " +
                    "   COUNT(ii.indentId)" +
                    ") " +
                    "FROM IndentInventory ii " +
                    "WHERE ii.indentStatus IN :statuses " +
                    "AND ii.isDeleted = false " +
                    "GROUP BY ii.indentStatus, ii.tenant"
    )
    List<StatusGroupCountDTO> fetchCurrentIndentStatusCounts(
            @Param("statuses") List<String> statuses
    );

    @Query(
            value =
                    "SELECT " +
                            "  ii.tenant AS tenant, " +
                            "  CASE " +
                            "    WHEN ii.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 'GT_30_DAYS' " +
                            "    WHEN ii.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 15 DAY) THEN 'GT_15_DAYS' " +
                            "    WHEN ii.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 7 DAY)  THEN 'GT_7_DAYS'  " +
                            "    WHEN ii.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 3 DAY)  THEN 'GT_3_DAYS'  " +
                            "  END AS bucket, " +
                            "  COUNT(*) AS cnt " +
                            "FROM indent_inventory ii " +
                            "WHERE ii.last_status_updated_at < DATE_SUB(NOW(), INTERVAL 3 DAY) " +
                            "  AND ii.indent_status NOT IN (:terminalStatuses) " +
                            "  AND ii.is_deleted = 0 " +
                            "GROUP BY ii.tenant, bucket",
            nativeQuery = true
    )
    List<Object[]> fetchStaleIndentBucketData(@Param("terminalStatuses") List<String> terminalStatuses);

    @Query(
        "SELECT new com.ec.application.data.IndentForDropdownDTO(" +
        "    i.indentId, i.indentDate, i.indentStatus, i.tenant) " +
        "FROM IndentInventory i " +
        "WHERE i.isDeleted = false " +
        "ORDER BY i.indentDate DESC"
    )
    List<IndentForDropdownDTO> findForDropdown(org.springframework.data.domain.Pageable pageable);

    // STEP 1: spec-aware, pagination-safe (NO EntityGraph)
    @Override
    Page<IndentInventory> findAll(Specification<IndentInventory> spec, Pageable pageable);

    // STEP 2: fetch full graph by IDs
    @EntityGraph(attributePaths = {
            "inventoryList",
            "inventoryList.product",
            "inventoryList.product.category",
            "fileInformations"
    })
    @Query("select distinct i from IndentInventory i where i.indentId in :ids")
    List<IndentInventory> findWithDetailsByIndentIdIn(@Param("ids") List<String> ids);

    @Query("SELECT DISTINCT i.requiredBy FROM IndentInventory i WHERE i.requiredBy IS NOT NULL AND i.requiredBy <> ''")
    List<String> findDistinctRequiredBy();

    @Query("SELECT COUNT(ii) FROM IndentInventory ii WHERE ii.isDeleted = false AND ii.indentDate >= :from " +
            "AND (:tenant IS NULL OR ii.tenant = :tenant)")
    long countSince(@Param("from") java.util.Date from, @Param("tenant") String tenant);

    @Query("SELECT COUNT(ii) FROM IndentInventory ii WHERE ii.isDeleted = false AND ii.indentStatus IN :statuses " +
            "AND (:tenant IS NULL OR ii.tenant = :tenant)")
    long countByStatusIn(@Param("statuses") java.util.List<String> statuses, @Param("tenant") String tenant);

    @Query("SELECT COUNT(ii) FROM IndentInventory ii WHERE ii.isDeleted = false " +
            "AND (:tenant IS NULL OR ii.tenant = :tenant) " +
            "AND EXISTS (SELECT l FROM IndentInventoryList l WHERE l.indentInventory = ii " +
            "            AND l.quoteRequestedQcId IS NOT NULL AND l.isDeleted = false)")
    long countWithQuoteRequested(@Param("tenant") String tenant);

    @Query("SELECT COUNT(ii) FROM IndentInventory ii WHERE ii.isDeleted = false " +
            "AND ii.indentStatus NOT IN :excluded AND ii.lastStatusUpdatedAt < :cutoff " +
            "AND (:tenant IS NULL OR ii.tenant = :tenant)")
    long countStale(@Param("cutoff") java.util.Date cutoff,
                    @Param("excluded") java.util.List<String> excluded,
                    @Param("tenant") String tenant);
}


