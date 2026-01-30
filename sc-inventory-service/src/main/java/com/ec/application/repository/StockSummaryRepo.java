package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.StockSummary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StockSummaryRepo extends BaseRepository<StockSummary, Long> {

    List<StockSummary> findByTenantSchema(String tenantSchema);

    @Query("SELECT MAX(d.syncedAt) FROM StockSummary d")
    Date findLastSyncTime();

    List<StockSummary> findByTenantSchemaAndProductIdAndWarehouseId(String tenantSchema, Long productId, Long warehouseId);

    @Query("SELECT d FROM StockSummary d WHERE d.productId IN :productIds")
    List<StockSummary> fetchStockByProductIds(@Param("productIds") List<Long> productIds);

    @Query("SELECT d FROM StockSummary d WHERE d.productId IN :productIds AND d.warehouseName = :warehouseName")
    List<StockSummary> fetchStockByProductIdsAndWarehouse(@Param("productIds") List<Long> productIds, @Param("warehouseName") String warehouseName);
}
