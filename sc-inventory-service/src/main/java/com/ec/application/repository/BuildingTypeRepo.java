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
import com.ec.application.model.BuildingType;

@Repository
public interface BuildingTypeRepo extends BaseRepository<BuildingType, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	BuildingType save(BuildingType entity);

	boolean existsBytypeName(String buildingTypeName);

	ArrayList<BuildingType> findBytypeName(@Param("typeName") String typeName);

	@Query(value = "SELECT m from BuildingType m where typeName LIKE %:name%")
	ArrayList<BuildingType> findByPartialName(@Param("name") String name);

	@Query(value = "SELECT typeId as id,typeName as name from BuildingType m  order by name")
	List<IdNameProjections> findIdAndNames();

	@Query(value = "SELECT typeName as name from BuildingType m where typeName like %:name% order by BuildingTypeName")
	List<String> getNames(@Param("name") String name);

	@Query(value = "SELECT typeName as name from BuildingType m order by typeName")
	List<String> getBuildingTypeNames();
}
