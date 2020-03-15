package com.ec.application.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Stock.Stock;

@Repository
public interface StockRepo  extends BaseRepository<Stock, Long>
{
	@Query(value="SELECT m from Stock m where m.product.category.categoryId=:categoryID")
	Page<Stock> findStockForCategory(Pageable pageable, Long categoryID);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId")
	Page<Stock> findStockForProduct(Pageable pageable, Long productId);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId and m.warehouse.warehouseName=:warehousename")
	List<Stock> findByIdName(Long productId,String warehousename);

	@Query(value="SELECT m from Stock m where m.product.productId=:productId")
	List<Stock> findStockForProductAsList(Long productId);
}
