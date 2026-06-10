package com.ec.application.repository;

import com.ec.application.model.GlobalFifoReport;
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
public interface GlobalFifoReportRepository
        extends JpaRepository<GlobalFifoReport, Long>,
                JpaSpecificationExecutor<GlobalFifoReport> {

    /** Last sync time for incremental sync — tracks how far we have synced for a given tenant. */
    @Query("SELECT MAX(r.syncedAt) FROM GlobalFifoReport r WHERE r.tenantSchema = :tenantSchema")
    Date findLastSyncTimeByTenantSchema(@Param("tenantSchema") String tenantSchema);

    /** Upsert helper — find existing row to decide insert vs update. */
    Optional<GlobalFifoReport> findByTenantSchemaAndOutwardIdAndBatchIdAndProductId(
            String tenantSchema, Long outwardId, Long batchId, Long productId);

    /** Remove a specific consumption row when the source record is soft-deleted. */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalFifoReport r WHERE r.tenantSchema = :tenantSchema AND r.outwardId = :outwardId AND r.batchId = :batchId AND r.productId = :productId")
    void deleteByTenantSchemaAndOutwardIdAndBatchIdAndProductId(
            @Param("tenantSchema") String tenantSchema,
            @Param("outwardId") Long outwardId,
            @Param("batchId") Long batchId,
            @Param("productId") Long productId);

    /** Tile stats — count distinct outward IDs by project for a given date window. */
    @Query(value = "SELECT tenantSchema, COUNT(DISTINCT outwardId) " +
           "FROM global_fifo_report WHERE outwardDate >= :from GROUP BY tenantSchema ORDER BY 2 DESC",
           nativeQuery = true)
    List<Object[]> countByProjectSince(@Param("from") Date from);

    @Query(value = "SELECT COUNT(DISTINCT outwardId) " +
           "FROM global_fifo_report WHERE outwardDate >= :from",
           nativeQuery = true)
    Long countTotalSince(@Param("from") Date from);

    @Query(value = "SELECT COUNT(DISTINCT productId) FROM global_fifo_report", nativeQuery = true)
    Long countDistinctProducts();

    @Query(value = "SELECT tenantSchema, COUNT(DISTINCT productId) FROM global_fifo_report " +
           "GROUP BY tenantSchema ORDER BY 2 DESC", nativeQuery = true)
    List<Object[]> countDistinctProductsByProject();

    /** Distinct productIds present in master for a given tenant — used by metadata refresh. */
    @Query("SELECT DISTINCT r.productId FROM GlobalFifoReport r WHERE r.tenantSchema = :tenantSchema")
    List<Long> findDistinctProductIdsByTenantSchema(@Param("tenantSchema") String tenantSchema);

    /** Bulk-update productName + measurementUnit for all rows of a given (tenant, product). */
    @Modifying
    @Transactional
    @Query("UPDATE GlobalFifoReport r SET r.productName = :productName, r.measurementUnit = :unit " +
           "WHERE r.tenantSchema = :tenantSchema AND r.productId = :productId")
    void updateProductMetadata(@Param("tenantSchema") String tenantSchema,
                               @Param("productId") Long productId,
                               @Param("productName") String productName,
                               @Param("unit") String unit);

    /** Distinct values for filter dropdowns. */
    @Query("SELECT DISTINCT r.productName FROM GlobalFifoReport r WHERE r.productName IS NOT NULL ORDER BY r.productName")
    List<String> findDistinctProductNames();

    @Query("SELECT DISTINCT r.contractorName FROM GlobalFifoReport r WHERE r.contractorName IS NOT NULL ORDER BY r.contractorName")
    List<String> findDistinctContractorNames();

    @Query("SELECT DISTINCT r.performedBy FROM GlobalFifoReport r WHERE r.performedBy IS NOT NULL ORDER BY r.performedBy")
    List<String> findDistinctPerformedBy();
}
