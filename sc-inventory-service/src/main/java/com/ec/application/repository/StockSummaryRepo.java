package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ProductStockSumDTO;
import com.ec.application.model.StockSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface StockSummaryRepo
        extends BaseRepository<StockSummary, Long>,
        StockSummaryCustomRepo {

    List<StockSummary> findByTenantSchema(String tenantSchema);

    @Query("SELECT MAX(d.syncedAt) FROM StockSummary d")
    Date findLastSyncTime();

    @Query("SELECT MAX(d.syncedAt) FROM StockSummary d where tenantSchema=:tenantSchema")
    Date findLastSyncTimeByTenantSchema(@Param("tenantSchema") String tenantSchema);

    List<StockSummary> findByTenantSchemaAndProductIdAndWarehouseId(String tenantSchema, Long productId, Long warehouseId);

    @Query("SELECT d FROM StockSummary d WHERE d.productId IN :productIds")
    List<StockSummary> fetchStockByProductIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT d FROM StockSummary d WHERE d.productId IN :productIds AND d.warehouseName = :warehouseName")
    List<StockSummary> fetchStockByProductIdsAndWarehouse(@Param("productIds") List<Long> productIds, @Param("warehouseName") String warehouseName);

    @Query("SELECT new com.ec.application.data.ProductStockSumDTO(d.productId, SUM(d.quantityInHand)) FROM StockSummary d " +
            "WHERE d.productId IN :productIds " +
            "GROUP BY d.productId")
    List<ProductStockSumDTO> fetchTotalStockByProductIds(
            @Param("productIds") List<Long> productIds
    );

    @Query(
            "SELECT s.productId, s.tenantSchema, SUM(s.quantityInHand) " +
                    "FROM StockSummary s " +
                    "WHERE s.productId IN :productIds " +
                    "GROUP BY s.productId, s.tenantSchema"
    )
    List<Object[]> fetchTenantWiseStockForProducts(@Param("productIds") List<Long> productIds);


    @Query(
            "SELECT s.productId, s.tenantSchema, SUM(s.quantityInHand) " +
                    "FROM StockSummary s " +
                    "WHERE s.productId IN :productIds " +
                    "AND s.warehouseName = :warehouseName " +
                    "GROUP BY s.productId, s.tenantSchema"
    )
    List<Object[]> fetchTenantWiseDeadStockForProducts(
            @Param("productIds") List<Long> productIds,
            @Param("warehouseName") String warehouseName
    );

    // Fetch all stock rows (non-dead-stock warehouses) for given productIds
    @Query("SELECT d FROM StockSummary d WHERE d.productId IN :productIds")
    List<StockSummary> fetchTotalStockByProductIdsExcludingWarehouse(
            @Param("productIds") List<Long> productIds);

    /**
     * Bulk-update reorder_level for all warehouse rows of a (tenantSchema, productId) pair.
     * Called by the hourly sync job.
     */
    @Modifying
    @Transactional
    @Query("UPDATE StockSummary s SET s.reorderLevel = :reorderLevel " +
           "WHERE s.tenantSchema = :tenantSchema AND s.productId = :productId")
    void updateReorderLevel(@Param("tenantSchema") String tenantSchema,
                            @Param("productId") Long productId,
                            @Param("reorderLevel") Double reorderLevel);

    /**
     * Bulk-update measurement_unit for all warehouse rows of a (tenantSchema, productId) pair.
     * Called by the hourly sync job to pick up Product-level measurement unit changes.
     */
    @Modifying
    @Transactional
    @Query("UPDATE StockSummary s SET s.measurementUnit = :measurementUnit " +
           "WHERE s.tenantSchema = :tenantSchema AND s.productId = :productId")
    void updateMeasurementUnit(@Param("tenantSchema") String tenantSchema,
                               @Param("productId") Long productId,
                               @Param("measurementUnit") String measurementUnit);

    /**
     * Returns all distinct productIds that already have a stock_summary row for the given tenant.
     * Used by the sync job to decide which products need a reorder-level update.
     */
    @Query("SELECT DISTINCT s.productId FROM StockSummary s WHERE s.tenantSchema = :tenantSchema")
    List<Long> findDistinctProductIdsByTenantSchema(@Param("tenantSchema") String tenantSchema);
}
