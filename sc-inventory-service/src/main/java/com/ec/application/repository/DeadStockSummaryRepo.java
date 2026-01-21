package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.DeadStockSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DeadStockSummaryRepo extends BaseRepository<DeadStockSummary, Long> {

    List<DeadStockSummary> findByTenantSchema(String tenantSchema);

    @Query("SELECT MAX(d.syncedAt) FROM DeadStockSummary d")
    Date findLastSyncTime();

    List<DeadStockSummary> findByTenantSchemaAndProductIdAndWarehouseId(String tenantSchema, Long productId, Long warehouseId);

    @Query("SELECT d FROM DeadStockSummary d WHERE d.productId IN :productIds")
    List<DeadStockSummary> fetchDeadStockByProductIds(@Param("productIds") List<Long> productIds);
}
