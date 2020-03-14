package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.UnloadingArea;

@Repository
public interface UnloadingAreaRepo extends BaseRepository<UnloadingArea, Long>
{
	boolean existsByUnloadingAreaName(String unloadingAreaName);

	ArrayList<UnloadingArea> findByUnloadingAreaName(String unloadingAreaName);

	@Query(value="SELECT m from UnloadingArea m where unloadingAreaName LIKE %:name%")
	ArrayList<UnloadingArea> findByPartialName(@Param("name") String name);

	@Query(value="SELECT unloadingAreaId as id,unloadingAreaName as name from UnloadingArea m")
	List<IdNameProjections> findIdAndNames();
}
