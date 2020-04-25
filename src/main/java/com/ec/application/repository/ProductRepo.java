package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Machinery;
import com.ec.application.model.Product;

@Repository
public interface ProductRepo extends BaseRepository<Product, Long>
{

	boolean existsByProductName(String productName);

	ArrayList<Product> findByproductName(String productName);

	@Query(value="SELECT m from Product m where m.category.categoryId=id")
	ArrayList<Product> existsByCategoryId(@Param("id")Long id);

	@Query(value="SELECT productId as id,productName as name from Product m")
	List<IdNameProjections> findIdAndNames();
	
	@Query(value="SELECT count(*) from Product m where m.category.categoryId=:categoryId")
	int categoryUsageCount(@Param("categoryId")Long categoryId);

	@Query(value="SELECT productName from Product m")
	List<String> getNames();

	@Query(value="SELECT distinct productId from Product m")
	List<Long> fetchUniqueProductIds();
}
