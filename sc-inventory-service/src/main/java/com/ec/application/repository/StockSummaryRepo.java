package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ProductStockSumDTO;
import com.ec.application.model.StockSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


}
