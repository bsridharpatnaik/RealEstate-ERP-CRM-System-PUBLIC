package com.ec.application.repository;

import com.ec.application.model.GlobalStockAgingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalStockAgingDetailRepository extends JpaRepository<GlobalStockAgingDetail, Long> {

    Optional<GlobalStockAgingDetail> findByTenantSchemaAndProductIdAndWarehouseId(
            String tenantSchema, Long productId, Long warehouseId);

    List<GlobalStockAgingDetail> findByTenantSchemaAndProductId(String tenantSchema, Long productId);

    /** Remove all detail rows for a product+tenant before re-inserting (handles warehouse removal). */
    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalStockAgingDetail d WHERE d.tenantSchema = :tenantSchema AND d.productId = :productId")
    void deleteByTenantSchemaAndProductId(@Param("tenantSchema") String tenantSchema,
                                          @Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GlobalStockAgingDetail d WHERE d.tenantSchema = :tenantSchema")
    void deleteByTenantSchema(@Param("tenantSchema") String tenantSchema);
}
