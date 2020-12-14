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
import com.ec.application.model.Category;

@Repository
public interface CategoryRepo extends BaseRepository<Category, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Category save(Category entity);

	boolean existsByCategoryName(String categoryName);

	ArrayList<Category> findBycategoryName(String categoryName);

	@Query(value = "SELECT m from Category m where categoryName LIKE %:name%")
	ArrayList<Category> findByPartialName(@Param("name") String name);

	@Query(value = "SELECT categoryId as id,categoryName as name from Category m  order by name")
	List<IdNameProjections> findIdAndNames();

	@Query(value = "SELECT categoryName as name from Category m where categoryName like %:name% order by categoryName")
	List<String> getNames(@Param("name") String name);

	@Query(value = "SELECT categoryName as name from Category m order by categoryName")
	List<String> getCategoryNames();

}
