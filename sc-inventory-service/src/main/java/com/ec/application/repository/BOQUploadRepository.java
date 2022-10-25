package com.ec.application.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.Assert;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.OutwardQuantityDtoForBoqStatus;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.Contact;

public interface BOQUploadRepository extends BaseRepository<BOQUpload, Long>{

	BOQUpload findByBuildingType(int buildingType);

	List<BOQUpload> findByUsageLocation(long buildingUnit);

	List<BOQUpload> findByUsageLocationLocationId(long buildingUnit);

	List<BOQUpload> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, long buildingUnitId);
		
	BOQUpload findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(long buildingUnit,
			long usageAreaId, long productId);

	@Query(value = "Select * from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2",nativeQuery = true)
	List<BOQUpload> findBOQQuantity(long buildingTypeId, long buildingUnitId);

	@Query(value = "Select Sum(quantity),buildingTypeId, usageLocationId from BOQUpload b where b.productId=?1 and b.buildingTypeId=?2 and b.usageLocationId=?3",nativeQuery = true)
	Double findQuantityByProductProductId(long productId, long buildingTypeId, long buildingUnitId);

	@Query(value="Select DISTINCT(productId), buildingTypeId,usageLocationId from BOQUpload b where is_deleted=false",nativeQuery = true)
	List<Object> findBuildigTypeIdBuildingUnitIdProductId();
	
	@Query(value="Select DISTINCT(productId) from BOQUpload b",nativeQuery = true)
	List<Integer> findByProductId();

	@Query(value="Select DISTINCT(productId),buildingTypeId,usageLocationId from BOQUpload b where b.buildingTypeId=?1",nativeQuery = true)
	List<Object> findByBuildingTypeTypeId(long buildingTypeId);
	
	@Query(value="Select DISTINCT(productId) from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2",nativeQuery = true)
	List<Integer> findProductIdByBuildingTypeTypeIdAndUsageLocationLocationIds(long buildingTypeId,long buildingUnitId);

	//**********
	@Query(value="select bu.buildingTypeId,bu.usageLocationId,bu.productId,sum(quantity),ul.location_name from boqupload bu, usage_location ul\r\n"
			+ " where \r\n"
			+ " bu.usageLocationId=ul.locationId and\r\n"
			+ " bu.buildingTypeId=?1 and bu.usageLocationId in(?2) and bu.is_deleted=false\r\n"
			+ "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId,ul.location_name" ,nativeQuery = true)
	List<Object> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, List<Long> buildingUnitIds);

	@Query(value="select sum(ioe.quantity) as outwardQuantity,bu.buildingTypeId, bu.usageLocationId,bu.productId\r\n"
			+ "	 from boqupload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, usage_location ul\r\n"
			+ "	 where\r\n"
			+ "	 bu.locationId=oi.usageAreaId and \r\n"
			+ "	 bu.usageLocationId = oi.locationId and\r\n"
			+ "	 bu.productId=ioe.productId and \r\n"
			+ "	 oe.outwardid = oi.outwardid and\r\n"
			+ "	 ioe.entryId = oe.entryId and\r\n"
			+ "	 ul.locationId = oi.locationId and\r\n"
			+ "	 bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and\r\n"
			+ "	 oi.is_deleted=false and bu.is_deleted=false\r\n"
			+ "	 GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId\r\n"
			+ "",nativeQuery = true)
	List<Object> findOutwardQuantityByBuildingTypeIdAndUsageLocationId(Set<Long> buildingTypeIDsForQuery, Set<Long> buildingUnitIDsForQuery);

	@Query(value="select bu.id,ua.usageAreaId,0.0 as ioequantity,bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name,bu.buildingTypeId ,bu.productId\r\n"
			+ " from boqupload bu,usage_area ua where bu.locationId=ua.usageAreaId and bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and bu.is_deleted=false and bu.id NOT IN (\r\n"
			+ "select bus.id from \r\n"
			+ "(select bu.id,oi.usageAreaId,sum(ioe.quantity),bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name,bu.buildingTypeId,bu.productId,bu.locationId\r\n"
			+ "	from boqupload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, usage_location ul,usage_area ua\r\n"
			+ "	where\r\n"
			+ " bu.locationId=oi.usageAreaId and \r\n"
			+ "	bu.usageLocationId = oi.locationId and\r\n"
			+ " oe.outwardid = oi.outwardid and\r\n"
			+ " ul.locationId = oi.locationId and\r\n"
			+ " ioe.entryId = oe.entryId and\r\n"
			+ "	bu.productId=ioe.productId and \r\n"
			+ "	bu.locationId=ua.usageAreaId and\r\n"
			+ "	bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and\r\n"
			+ "	oi.is_deleted=false and bu.is_deleted=false \r\n"
			+ "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId,bu.locationId,bu.id,oi.usageAreaId,ioe.productId,bu.quantity,ua.usagearea_name) bus\r\n"
			+ "	)\r\n"
			+ "	UNION\r\n"
			+ "select bu.id,oi.usageAreaId,sum(ioe.quantity) as ioequantity ,bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name, bu.buildingTypeId,bu.productId\r\n"
			+ "	from boqupload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, usage_location ul,usage_area ua\r\n"
			+ "	where\r\n"
			+ " bu.locationId=oi.usageAreaId and \r\n"
			+ "	bu.usageLocationId = oi.locationId and\r\n"
			+ " oe.outwardid = oi.outwardid and\r\n"
			+ " ul.locationId = oi.locationId and\r\n"
			+ " ioe.entryId = oe.entryId and\r\n"
			+ "	bu.productId=ioe.productId and \r\n"
			+ "	bu.locationId=ua.usageAreaId and\r\n"
			+ "	bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and\r\n"
			+ "	oi.is_deleted=false and bu.is_deleted=false\r\n"
			+ "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId ,ioe.productId,oi.usageAreaId,bu.quantity,ua.usagearea_name,bu.id\r\n" , nativeQuery = true)
	List<Object> findUsageAreaBoqQuantityOutwardQuantityByBuildingTypeIdAndUsageLocationId(Set<Long> buildingTypeIDsForQuery,
			Set<Long> buildingUnitIDsForQuery);
	
}
