package com.ec.application.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Stock;

@Repository
public interface StockRepo  extends BaseRepository<Stock, Long>
{
	@Query(value="SELECT m from Stock m where m.product.category.categoryId=:categoryId")
	Page<Stock> findStockForCategory(Pageable pageable, @Param("categoryId")Long categoryId);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId")
	Page<Stock> findStockForProduct(Pageable pageable, @Param("productId")Long productId);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId and m.warehouse.warehouseName=:warehousename")
	List<Stock> findByIdName(@Param("productId")Long productId,@Param("warehousename") String warehousename);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId")
	List<Stock> findStockForProductAsList(@Param("productId")Long productId);

	@Query(value="SELECT count(*) from Stock m where m.product.productId=:productId")
	int productUsageCount(@Param("productId")Long productId);

	@Query(value="SELECT count(*) from Stock m where m.warehouse.warehouseName=:warehouseName")
	int warehouseCount(@Param("warehouseName")String warehouseName);
}
