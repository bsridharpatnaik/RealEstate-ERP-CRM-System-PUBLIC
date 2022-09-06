package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.Assert;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.Contact;

public interface BOQUploadRepository extends BaseRepository<BOQUpload, Long>{

	BOQUpload findByBuildingType(int buildingType);

	List<BOQUpload> findByUsageLocation(long buildingUnit);

	List<BOQUpload> findByUsageLocationLocationId(long buildingUnit);

	List<BOQUpload> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, long buildingUnitId);
	
	@Query(value="Select DISTINCT(product.productId) from BOQUpload b where b.buildingType.typeId=:buildingTypeId and b.usageLocation.locationId=:buildingUnitId")
	List<Integer> findProductIdByBuildingTypeTypeIdAndUsageLocationLocationIds(@Param("buildingTypeId") long buildingTypeId,@Param("buildingUnitId") long buildingUnitId);
	
	BOQUpload findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(long buildingUnit,
			long usageAreaId, long productId);

	@Query(value = "Select * from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2",nativeQuery = true)
	List<BOQUpload> findBOQQuantity(long buildingTypeId, long buildingUnitId);

	@Query(value = "Select Sum(quantity) as quantity from BOQUpload b where b.productId=?1 and b.buildingTypeId=?2 and b.usageLocationId=?3",nativeQuery = true)
	Double findQuantityByProductProductId(long l, long buildingTypeId, long buildingUnitId);

	@Query(value="Select DISTINCT(productId), buildingTypeId,usageLocationId from BOQUpload b where is_deleted=false",nativeQuery = true)
	List<Object> findBuildigTypeIdBuildingUnitIdProductId();
	
	@Query(value="Select DISTINCT(productId) from BOQUpload b",nativeQuery = true)
	List<Integer> findByProductId();

	
}
