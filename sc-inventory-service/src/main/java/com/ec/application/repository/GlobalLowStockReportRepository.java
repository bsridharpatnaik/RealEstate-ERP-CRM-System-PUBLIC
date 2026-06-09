package com.ec.application.repository;

import com.ec.application.model.GlobalLowStockReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalLowStockReportRepository
        extends JpaRepository<GlobalLowStockReport, Long>,
                JpaSpecificationExecutor<GlobalLowStockReport> {

    Optional<GlobalLowStockReport> findByTenantSchemaAndProductId(String tenantSchema, Long productId);

    /** Remove products that recovered (no longer low stock) for a given tenant. */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalLowStockReport r " +
           "WHERE r.tenantSchema = :tenantSchema AND r.productId NOT IN :lowStockProductIds")
    void deleteRecoveredProducts(@Param("tenantSchema") String tenantSchema,
                                 @Param("lowStockProductIds") List<Long> lowStockProductIds);

    /** Remove all rows for a tenant (used when tenant has zero low-stock products). */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalLowStockReport r WHERE r.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);

    /** Tile counts — products that became low stock within a time window. */
    @Query("SELECT COUNT(r) FROM GlobalLowStockReport r WHERE r.lowStockSince >= :since")
    Long countSince(@Param("since") Date since);

    @Query("SELECT r.tenantSchema, COUNT(r) FROM GlobalLowStockReport r " +
           "WHERE r.lowStockSince >= :since GROUP BY r.tenantSchema ORDER BY COUNT(r) DESC")
    List<Object[]> countByProjectSince(@Param("since") Date since);

    /** Total currently low-stock products (all tenants). */
    @Query("SELECT COUNT(r) FROM GlobalLowStockReport r")
    Long countAll();

    @Query("SELECT r.tenantSchema, COUNT(r) FROM GlobalLowStockReport r " +
           "GROUP BY r.tenantSchema ORDER BY COUNT(r) DESC")
    List<Object[]> countAllByProject();

    /** Dropdown values. */
    @Query("SELECT DISTINCT r.category FROM GlobalLowStockReport r " +
           "WHERE r.category IS NOT NULL ORDER BY r.category")
    List<String> findDistinctCategories();
}
