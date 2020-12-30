package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.BOQStatusLocationsForType;
import com.ec.application.model.BOQStatus;

@Repository
public interface BOQStatusRepo extends BaseRepository<BOQStatus, String>
{

	@Query(value = "SELECT COUNT(DISTINCT locationId) from BOQStatus m  where m.typeId=:typeId AND m.locationId IS NOT NULL AND m.consumedPercent>100")
	Long getBOQCrossedCountForBT(@Param("typeId") Long typeId);

	@Query(value = "SELECT COUNT(DISTINCT locationId) from BOQStatus m  where m.typeId=:typeId AND m.locationId IS NOT NULL")
	Long getBOQTotalCountForBT(Long typeId);

	@Query(value = "SELECT new com.ec.application.data.BOQStatusLocationsForType(m.locationId,m.locationName,case WHEN max(consumedPercent)>100 THEN true else false end) "
			+ "from BOQStatus m  where m.typeId=:typeId AND m.locationId IS NOT NULL GROUP BY m.locationId,m.locationName")
	List<BOQStatusLocationsForType> getLocationWiseStatusForType(Long typeId);
}
