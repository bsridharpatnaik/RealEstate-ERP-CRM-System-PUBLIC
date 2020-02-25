package com.ec.application.repository;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Product;
import com.ec.application.model.Machinery;

@Repository
public interface ProductRepo extends BaseRepository<Product, Long>
{

	boolean existsByProductName(String productName);

	ArrayList<Product> findByproductName(String productName);

	@Query(value="SELECT m from Product m where productName LIKE %:name%")
	ArrayList<Product> findByPartialName(String name);
}
