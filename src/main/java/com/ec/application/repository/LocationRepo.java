package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.Location;

@Repository
public interface LocationRepo extends BaseRepository<Location, Long>
{

	boolean existsByLocationName(String locationName);

	ArrayList<Location> findByLocationName(String locationName);

	@Query(value="SELECT m from Location m where locationName LIKE %:name%")
	ArrayList<Location> findByPartialName(@Param("name") String name);

	@Query(value="SELECT locationId as id,locationName as name from Location m")
	List<IdNameProjections> findIdAndNames();
}
