package com.ec.application.repository;

import com.ec.application.model.GlobalStockAgingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalStockAgingReportRepository
        extends JpaRepository<GlobalStockAgingReport, Long>,
                JpaSpecificationExecutor<GlobalStockAgingReport> {

    Optional<GlobalStockAgingReport> findByTenantSchemaAndProductId(String tenantSchema, Long productId);

    /** Used to soft-delete zero-stock rows per tenant during sync. */
    @Modifying
    @Transactional
    @Query("UPDATE GlobalStockAgingReport r SET r.isDeleted = true " +
           "WHERE r.tenantSchema = :tenantSchema AND r.productId NOT IN :activeProductIds")
    void markDeletedForTenant(@Param("tenantSchema") String tenantSchema,
                              @Param("activeProductIds") List<Long> activeProductIds);

    /** Delete all rows for a tenant (used on first-sync or re-sync). */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalStockAgingReport r WHERE r.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);

    /** Bucket counts for tiles — only non-deleted rows. */
    @Query("SELECT r.agingBucket, COUNT(r) FROM GlobalStockAgingReport r " +
           "WHERE r.isDeleted = false GROUP BY r.agingBucket")
    List<Object[]> countByAgingBucket();

    @Query("SELECT r.agingBucket, COUNT(r) FROM GlobalStockAgingReport r " +
           "WHERE r.isDeleted = false AND r.tenantSchema = :tenantSchema GROUP BY r.agingBucket")
    List<Object[]> countByAgingBucketForTenant(@Param("tenantSchema") String tenantSchema);

    /** Distinct values for filter dropdowns. */
    @Query("SELECT DISTINCT r.productName FROM GlobalStockAgingReport r " +
           "WHERE r.productName IS NOT NULL AND r.isDeleted = false ORDER BY r.productName")
    List<String> findDistinctProductNames();

    @Query("SELECT DISTINCT r.category FROM GlobalStockAgingReport r " +
           "WHERE r.category IS NOT NULL AND r.isDeleted = false ORDER BY r.category")
    List<String> findDistinctCategories();
}
