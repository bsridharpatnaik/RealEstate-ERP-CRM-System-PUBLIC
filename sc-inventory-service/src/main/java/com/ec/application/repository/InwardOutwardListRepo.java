package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardOutwardList;

@Repository
public interface InwardOutwardListRepo extends BaseRepository<InwardOutwardList, Long>
{
	
	@Query(value="SELECT count(*) from InwardOutwardList m where m.product.productId=:productId")
	int productUsageCount(Long productId);

	@Query(value="select outward_inventory.outwardid,outward_inventory.locationId,Usage_Location.typeId,outwardinventory_entry.entryId,inward_outward_entries.productId,inward_outward_entries.quantity from outward_inventory \n"
			+ "JOIN outwardinventory_entry on outward_inventory.outwardid=outwardinventory_entry.outwardid \n"
			+ "JOIN inward_outward_entries on outwardinventory_entry.entryId=inward_outward_entries.entryId\n"
			+ "JOIN Usage_Location on outward_inventory.locationId=Usage_Location.locationId where Usage_Location.typeId=?1 and Usage_Location.locationId=?2 and inward_outward_entries.productId=?3\n"
			+ "and outward_inventory.is_deleted=false",nativeQuery = true)
	List<Object> findByOutwardInventory(long buildingTypeId, long businessUnitId, long productId);

}
