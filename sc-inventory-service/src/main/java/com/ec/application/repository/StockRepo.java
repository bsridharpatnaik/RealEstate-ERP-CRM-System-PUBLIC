package com.ec.application.repository;

import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.data.StockPercentData;
import com.ec.application.model.Stock;

@Repository
public interface StockRepo extends BaseRepository<Stock, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Stock save(Stock entity);

	@Query(value = "SELECT m from Stock m where m.product.category.categoryId=:categoryId")
	Page<Stock> findStockForCategory(Pageable pageable, @Param("categoryId") Long categoryId);

	@Query(value = "SELECT m from Stock m where m.product.productId=:productId")
	Page<Stock> findStockForProduct(Pageable pageable, @Param("productId") Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "SELECT m from Stock m where m.product.productId=:productId and m.warehouse.warehouseName=:warehousename")
	List<Stock> findByIdName(@Param("productId") Long productId, @Param("warehousename") String warehousename);

	@Query(value = "SELECT m from Stock m where m.product.productId=:productId and m.warehouse.warehouseName=:warehousename")
	List<Stock> findStockForProductAndWarehouse(@Param("productId") Long productId,
			@Param("warehousename") String warehousename);

	@Query(value = "SELECT count(*) from Stock m where m.product.productId=:productId")
	int productUsageCount(@Param("productId") Long productId);

	@Query(value = "SELECT count(*) from Stock m where m.warehouse.warehouseName=:warehouseName")
	int warehouseUsageCount(@Param("warehouseName") String warehouseName);

	@Query(value = "SELECT m from Stock m where m.warehouse.warehouseId=:warehouseId")
	Page<Stock> findStockForWarehouse(Pageable pageable, @Param("warehouseId") Long warehouseId);

	@Query(value = "SELECT SUM(quantityInHand) from Stock m where m.product.productId=:productId")
	Double getTotalStockForProduct(@Param("productId") Long productId);

	@Query(value = "SELECT SUM(quantityInHand) from Stock m where m.product.productId=:productId and m.warehouse.warehouseId=:warehouseId")
	Double getCurrentStockForProductWarehouse(@Param("productId") Long productId,
			@Param("warehouseId") Long warehouseId);

	@Query(value = "SELECT new com.ec.application.ReusableClasses.ProductIdAndStockProjection(m.product.productId, SUM(quantityInHand)) from Stock m "
			+ "where m.product.productId IN :productIds and m.warehouse.warehouseId=:warehouseId group by m.product.productId")
	List<ProductIdAndStockProjection> getCurrentStockForProductListWarehouse(@Param("productIds") List<Long> productIds,
			@Param("warehouseId") Long warehouseId);

	@Query(value = "SELECT ROUND(SUM(quantityInHand)) from Stock m where m.product.productId=:productId")
	Double getCurrentTotalStockForProduct(@Param("productId") Long productId);

	@Query(value = "SELECT new com.ec.application.data.StockPercentData(m.product.productId,m.product.productName,m.lastModifiedDate,SUM(m.quantityInHand)/m.product.reorderQuantity*100) from Stock m"
			+ " group by m.product.productId,m.product.productName,m.lastModifiedDate")
	List<StockPercentData> getCurrentStockPercent();
}
