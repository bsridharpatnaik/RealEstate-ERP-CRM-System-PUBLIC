package com.ec.application.repository;

import com.ec.application.model.GlobalStockAgingChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GlobalStockAgingChunkRepository extends JpaRepository<GlobalStockAgingChunk, Long> {

    List<GlobalStockAgingChunk> findByTenantSchemaAndProductIdAndWarehouseIdOrderBySortOrderAsc(
            String tenantSchema, Long productId, Long warehouseId);

    List<GlobalStockAgingChunk> findByTenantSchemaAndProductIdOrderByWarehouseIdAscSortOrderAsc(
            String tenantSchema, Long productId);

    /** Remove all chunks for a product+tenant before re-inserting (full resync each run). */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalStockAgingChunk c WHERE c.tenantSchema = :tenantSchema AND c.productId = :productId")
    void deleteByTenantSchemaAndProductId(@Param("tenantSchema") String tenantSchema,
                                          @Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalStockAgingChunk c WHERE c.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);
}
