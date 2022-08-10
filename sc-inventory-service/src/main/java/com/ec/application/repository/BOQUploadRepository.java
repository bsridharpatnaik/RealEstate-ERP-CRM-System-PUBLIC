package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.BOQUpload;

public interface BOQUploadRepository extends BaseRepository<BOQUpload, Long>{

	BOQUpload findByBuildingType(int buildingType);

	List<BOQUpload> findByUsageLocation(long buildingUnit);

	List<BOQUpload> findByUsageLocationLocationId(long buildingUnit);

	List<BOQUpload> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, long buildingUnitId);
	
	@Query(value="Select DISTINCT(productId) from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2",nativeQuery = true)
	List<Integer> findProductIdByBuildingTypeTypeIdAndUsageLocationLocationIds(long buildingTypeId,long buildingUnitId);

	BOQUpload findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(long buildingUnit,
			long usageAreaId, long productId);

	@Query(value = "Select Sum(quantity) as quantity from BOQUpload b where b.productId=?1 and b.buildingTypeId=?2 and b.usageLocationId=?3",nativeQuery = true)
	Double findQuantityByProductProductId(long l, long buildingTypeId, long buildingUnitId);

	@Query(value="SELECT buildingTypeId,usageLocationId,productId, COUNT(*) FROM BOQUpload GROUP BY buildingTypeId, usageLocationId,productId\r\n"
			+ "HAVING COUNT(*) > 0",nativeQuery = true )
	List<Object> findBuildigTypeIdBuildingUnitIdProductId();
	
	@Query(value="Select DISTINCT(productId) from BOQUpload b",nativeQuery = true)
	List<Integer> findByProductId();
	
}
