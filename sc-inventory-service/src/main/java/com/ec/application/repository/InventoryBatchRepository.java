package com.ec.application.repository;

import com.ec.application.model.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    // FIFO: oldest batches first with remaining qty > 0
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "ORDER BY b.receivedDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrder(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // All batches for a product+warehouse (for stock breakdown view)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.isDeleted = false " +
           "ORDER BY b.receivedDate ASC, b.batchId ASC")
    List<InventoryBatch> findAllBatchesForProduct(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // For expiry alerts — batches expiring within N days with qty remaining
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate IS NOT NULL " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "AND b.expiryDate BETWEEN :from AND :to")
    List<InventoryBatch> findBatchesExpiringBetween(
            @Param("from") Date from,
            @Param("to") Date to);

    // For expiry alert — already expired batches with qty remaining
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate IS NOT NULL " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "AND b.expiryDate <= :today")
    List<InventoryBatch> findExpiredBatchesWithStock(@Param("today") Date today);

    // Count distinct products expiring within N days (for tiles)
    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate BETWEEN :from AND :to")
    Long countDistinctProductsExpiringBetween(@Param("from") Date from, @Param("to") Date to);

    // Count distinct products with expired stock
    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate <= :today")
    Long countDistinctProductsExpired(@Param("today") Date today);

    // Product IDs expiring within N days (for tile filter)
    @Query("SELECT DISTINCT b.product.productId FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate BETWEEN :from AND :to")
    List<Long> findProductIdsExpiringBetween(@Param("from") Date from, @Param("to") Date to);

    // Product IDs with expired stock (for tile filter)
    @Query("SELECT DISTINCT b.product.productId FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate <= :today")
    List<Long> findProductIdsWithExpiredStock(@Param("today") Date today);

    // Pessimistic-locked FIFO query — use during outward consumption to prevent race conditions (#3)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "ORDER BY b.receivedDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrderLocked(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // Pessimistic-locked single batch fetch — use during write-off (#3)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.batchId = :id AND b.isDeleted = false")
    Optional<InventoryBatch> findByIdLocked(@Param("id") Long id);

    // Find batch by inward + product — used when rejecting an inward to reduce batch qty (#5)
    @Query("SELECT b FROM InventoryBatch b WHERE b.inwardId = :inwardId " +
           "AND b.product.productId = :productId AND b.isDeleted = false")
    Optional<InventoryBatch> findByInwardIdAndProductId(
            @Param("inwardId") Long inwardId,
            @Param("productId") Long productId);
}
