package com.ec.application.repository;

import com.ec.application.model.GlobalDeadStockReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GlobalDeadStockReportRepository
        extends JpaRepository<GlobalDeadStockReport, Long>,
                JpaSpecificationExecutor<GlobalDeadStockReport> {

    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalDeadStockReport r WHERE r.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);

    List<GlobalDeadStockReport> findByTenantSchema(String tenantSchema);

    @Query("SELECT DISTINCT r.tenantSchema FROM GlobalDeadStockReport r")
    List<String> findDistinctTenantSchemas();

    @Query("SELECT DISTINCT r.productName FROM GlobalDeadStockReport r ORDER BY r.productName")
    List<String> findDistinctProductNames();

    @Query("SELECT DISTINCT r.category FROM GlobalDeadStockReport r WHERE r.category IS NOT NULL ORDER BY r.category")
    List<String> findDistinctCategories();
}
