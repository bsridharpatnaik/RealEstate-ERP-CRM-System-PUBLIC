package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.UsageArea;

@Repository
public interface UsageAreaRepo extends BaseRepository<UsageArea, Long>
{

	boolean existsByUsageAreaName(String usageAreaName);

	ArrayList<UsageArea> findByusageAreaName(String usageAreaName);

	@Query(value="SELECT m from UsageArea m where usageAreaName LIKE %:name%")
	ArrayList<UsageArea> findByPartialName(@Param("name") String name);

	@Query(value="SELECT usageAreaId as id,usageAreaName as name from UsageArea m")
	List<IdNameProjections> findIdAndNames();

	@Query(value="SELECT usageAreaName as name from UsageArea m")
	List<String> getNames();

	
}
