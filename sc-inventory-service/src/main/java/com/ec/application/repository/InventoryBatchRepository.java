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
           "ORDER BY b.expiryDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrder(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // All batches for a product+warehouse (for stock breakdown view)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.isDeleted = false " +
           "ORDER BY b.expiryDate ASC, b.batchId ASC")
    List<InventoryBatch> findAllBatchesForProduct(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // All batches for a product across all warehouses
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.isDeleted = false " +
           "ORDER BY b.expiryDate ASC, b.batchId ASC")
    List<InventoryBatch> findAllBatchesForProductAllWarehouses(
            @Param("productId") Long productId);

    // For expiry alerts — batches expiring within N days with qty remaining
    // Use >= :from AND < :to (exclusive upper bound) to prevent day-boundary overlap:
    //   expired  = expiryDate < today  (strictly in the past)
    //   alert-30 = today <= expiryDate < today+30  (today..+29 days)
    //   alert-60 = today+30 <= expiryDate < today+60  (day 30..+59)
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate IS NOT NULL " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "AND b.expiryDate >= :from AND b.expiryDate < :to")
    List<InventoryBatch> findBatchesExpiringBetween(
            @Param("from") Date from,
            @Param("to") Date to);

    // Expired: strictly before today (expiryDate < today)
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate IS NOT NULL " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "AND b.expiryDate < :today")
    List<InventoryBatch> findExpiredBatchesWithStock(@Param("today") Date today);

    // Count distinct products expiring in [from, to) — exclusive upper bound
    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate >= :from AND b.expiryDate < :to")
    Long countDistinctProductsExpiringBetween(@Param("from") Date from, @Param("to") Date to);

    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate >= :from AND b.expiryDate < :to " +
           "AND b.product.productId IN :ids")
    Long countDistinctProductsExpiringBetweenIn(@Param("from") Date from, @Param("to") Date to, @Param("ids") List<Long> ids);

    // Count distinct products with expired stock (strictly before today)
    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate < :today")
    Long countDistinctProductsExpired(@Param("today") Date today);

    @Query("SELECT COUNT(DISTINCT b.product.productId) FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate < :today " +
           "AND b.product.productId IN :ids")
    Long countDistinctProductsExpiredIn(@Param("today") Date today, @Param("ids") List<Long> ids);

    // Product IDs expiring in [from, to) — exclusive upper bound
    @Query("SELECT DISTINCT b.product.productId FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate >= :from AND b.expiryDate < :to")
    List<Long> findProductIdsExpiringBetween(@Param("from") Date from, @Param("to") Date to);

    // Product IDs with expired stock (strictly before today)
    @Query("SELECT DISTINCT b.product.productId FROM InventoryBatch b " +
           "WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 " +
           "AND b.isDeleted = false AND b.expiryDate < :today")
    List<Long> findProductIdsWithExpiredStock(@Param("today") Date today);

    // ── BATCH_ONLY: FIFO by receivedDate (no expiry column required) ────────────

    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "ORDER BY b.receivedDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrderByReceived(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "ORDER BY b.receivedDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrderByReceivedLocked(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // ── BATCH_WITH_EXPIRY: FEFO by expiryDate (pessimistic-locked for outward) ──

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.qtyRemaining > 0 AND b.isDeleted = false " +
           "ORDER BY b.expiryDate ASC, b.batchId ASC")
    List<InventoryBatch> findAvailableBatchesFifoOrderLocked(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // Pessimistic-locked single batch fetch — use during write-off (#3)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.batchId = :id AND b.isDeleted = false")
    Optional<InventoryBatch> findByIdLocked(@Param("id") Long id);

    // Find batch by inward + product (single, legacy — prefer findAllByInwardIdAndProductId)
    @Query("SELECT b FROM InventoryBatch b WHERE b.inwardId = :inwardId " +
           "AND b.product.productId = :productId AND b.isDeleted = false")
    Optional<InventoryBatch> findByInwardIdAndProductId(
            @Param("inwardId") Long inwardId,
            @Param("productId") Long productId);

    // All batches for an inward + product — supports multiple splits per line
    @Query("SELECT b FROM InventoryBatch b WHERE b.inwardId = :inwardId " +
           "AND b.product.productId = :productId AND b.isDeleted = false " +
           "ORDER BY b.batchId ASC")
    List<InventoryBatch> findAllByInwardIdAndProductId(
            @Param("inwardId") Long inwardId,
            @Param("productId") Long productId);

    // All batches for an inward — used during delete/zeroing
    @Query("SELECT b FROM InventoryBatch b WHERE b.inwardId = :inwardId AND b.isDeleted = false")
    List<InventoryBatch> findAllByInwardId(@Param("inwardId") Long inwardId);

    // Total tracked qty for a product+warehouse — used to compute untracked stock during split
    @Query("SELECT COALESCE(SUM(b.qtyRemaining), 0.0) FROM InventoryBatch b " +
           "WHERE b.product.productId = :productId AND b.warehouse.warehouseId = :warehouseId " +
           "AND b.isDeleted = false")
    Double sumQtyRemainingByProductAndWarehouse(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // Sum of qtyRemaining per product — for untracked stock tile
    @Query("SELECT b.product.productId, COALESCE(SUM(b.qtyRemaining), 0.0) FROM InventoryBatch b " +
           "WHERE b.product.productId IN :ids AND b.isDeleted = false " +
           "GROUP BY b.product.productId")
    List<Object[]> sumQtyRemainingGroupByProduct(@Param("ids") List<Long> ids);

    // Sum of qtyRemaining per product+warehouse — for accurate per-warehouse untracked calculation
    @Query("SELECT b.product.productId, b.warehouse.warehouseId, COALESCE(SUM(b.qtyRemaining), 0.0) " +
           "FROM InventoryBatch b " +
           "WHERE b.product.productId IN :ids AND b.warehouse.warehouseId IN :warehouseIds AND b.isDeleted = false " +
           "GROUP BY b.product.productId, b.warehouse.warehouseId")
    List<Object[]> sumQtyRemainingGroupByProductAndWarehouse(
            @Param("ids") List<Long> productIds,
            @Param("warehouseIds") List<Long> warehouseIds);

    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.warehouse.warehouseName = :warehouseName AND b.isDeleted = false " +
           "AND b.qtyRemaining > 0")
    List<InventoryBatch> findActiveByWarehouseName(@Param("warehouseName") String warehouseName);

    /** All active batches with a non-null expiryDate and remaining stock — for expired stock report sync */
    @Query("SELECT b FROM InventoryBatch b WHERE b.expiryDate IS NOT NULL AND b.qtyRemaining > 0 AND b.isDeleted = false")
    List<InventoryBatch> findAllWithExpiryAndStock();

    /** Count of any non-deleted batches for a product — used to block batch-mode reversal. */
    @Query("SELECT COUNT(b) FROM InventoryBatch b WHERE b.product.productId = :productId AND b.isDeleted = false")
    long countByProductId(@Param("productId") Long productId);

    /** All non-deleted batches for a product+warehouse — used for upsert matching on transfer. */
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.productId = :productId " +
           "AND b.warehouse.warehouseId = :warehouseId AND b.isDeleted = false")
    List<InventoryBatch> findAllActiveByProductAndWarehouse(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);
}
