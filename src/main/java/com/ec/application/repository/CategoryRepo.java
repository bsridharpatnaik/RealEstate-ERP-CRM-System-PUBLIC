package com.ec.application.repository;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Category;
import com.ec.application.model.Machinery;

@Repository
public interface CategoryRepo extends BaseRepository<Category, Long>
{

	boolean existsByCategoryName(String categoryName);

	ArrayList<Category> findBycategoryName(String categoryName);

	@Query(value="SELECT m from Category m where categoryName LIKE %:name%")
	ArrayList<Category> findByPartialName(String name);
}
