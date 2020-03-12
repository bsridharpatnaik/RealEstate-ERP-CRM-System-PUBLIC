package com.ec.application.repository;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.Machinery;
import com.ec.application.model.BasicEntities.Product;

@Repository
public interface ProductRepo extends BaseRepository<Product, Long>
{

	boolean existsByProductName(String productName);

	ArrayList<Product> findByproductName(String productName);

	@Query(value="SELECT m from Product m where productName LIKE %:name%")
	ArrayList<Product> findByPartialName(@Param("name") String name);

	@Query(value="SELECT m from Product m where m.category.categoryName=categoryname")
	ArrayList<String> returnNameByCategory(String categoryname);

	@Query(value="SELECT m from Product m where m.category.categoryId=id")
	ArrayList<Product> existsByCategoryId(Long id);

	@Query(value="SELECT productId,productName from Product m")
	ArrayList<?> findIdAndNames();
}
