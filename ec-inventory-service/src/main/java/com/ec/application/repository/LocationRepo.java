package com.ec.application.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.UsageLocation;

@Repository
public interface LocationRepo extends BaseRepository<UsageLocation, Long>
{

	boolean existsByLocationName(String locationName);

	ArrayList<UsageLocation> findByLocationName(String locationName);

	@Query(value="SELECT m from UsageLocation m where locationName LIKE %:name%")
	ArrayList<UsageLocation> findByPartialName(@Param("name") String name);

	@Query(value="SELECT locationId as id,locationName as name from UsageLocation m")
	List<IdNameProjections> findIdAndNames();

	@Query(value="SELECT locationName from UsageLocation m")
	List<String> getNames();

}
