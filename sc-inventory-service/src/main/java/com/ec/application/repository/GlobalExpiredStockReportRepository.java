package com.ec.application.repository;

import com.ec.application.model.GlobalExpiredStockReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GlobalExpiredStockReportRepository
        extends JpaRepository<GlobalExpiredStockReport, Long>,
                JpaSpecificationExecutor<GlobalExpiredStockReport> {

    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalExpiredStockReport r WHERE r.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);

    /** Batches already expired (daysUntilExpiry < 0), filtered by allowed schemas. */
    @Query("SELECT COUNT(r) FROM GlobalExpiredStockReport r WHERE r.daysUntilExpiry < 0 AND r.tenantSchema IN :schemas")
    Long countExpired(@Param("schemas") List<String> schemas);

    /** Batches expiring within N days inclusive, filtered by allowed schemas. */
    @Query("SELECT COUNT(r) FROM GlobalExpiredStockReport r WHERE r.daysUntilExpiry <= :maxDays AND r.tenantSchema IN :schemas")
    Long countWithinDays(@Param("maxDays") int maxDays, @Param("schemas") List<String> schemas);

    @Query("SELECT DISTINCT r.category FROM GlobalExpiredStockReport r WHERE r.category IS NOT NULL ORDER BY r.category")
    List<String> findDistinctCategories();
}
