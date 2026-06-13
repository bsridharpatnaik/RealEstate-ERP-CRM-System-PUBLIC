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

    /** Batches already expired (daysUntilExpiry < 0) */
    @Query("SELECT COUNT(r) FROM GlobalExpiredStockReport r WHERE r.daysUntilExpiry < 0")
    Long countExpired();

    /** Batches expiring within N days inclusive (cumulative — includes expired when maxDays >= 0) */
    @Query("SELECT COUNT(r) FROM GlobalExpiredStockReport r WHERE r.daysUntilExpiry <= :maxDays")
    Long countWithinDays(@Param("maxDays") int maxDays);

    @Query("SELECT DISTINCT r.category FROM GlobalExpiredStockReport r WHERE r.category IS NOT NULL ORDER BY r.category")
    List<String> findDistinctCategories();
}
