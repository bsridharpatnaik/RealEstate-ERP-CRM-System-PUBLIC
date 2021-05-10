package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.IdNameAndUnit;
import com.ec.application.model.Product;

@Repository
public interface ProductRepo extends BaseRepository<Product, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Product save(Product entity);

	boolean existsByProductName(String productName);

	ArrayList<Product> findByproductName(String productName);

	@Query(value = "SELECT m from Product m where m.category.categoryId=id")
	ArrayList<Product> existsByCategoryId(@Param("id") Long id);

	@Query(value = "SELECT productId as id,productName as name from Product m  order by name")
	List<IdNameProjections> findIdAndNames();

	@Query(value = "SELECT count(*) from Product m where m.category.categoryId=:categoryId")
	int categoryUsageCount(@Param("categoryId") Long categoryId);

	@Query(value = "SELECT productName from Product m where productName like %:name% order by productName")
	List<String> getNames(@Param("name") String name);

	@Query(value = "SELECT distinct productId from Product m")
	List<Long> fetchUniqueProductIds();

	@Query(value = "SELECT new com.ec.application.data.IdNameAndUnit(productId,productName,measurementUnit) from Product m")
	List<IdNameAndUnit> getProductMeasurementUnit();

}
