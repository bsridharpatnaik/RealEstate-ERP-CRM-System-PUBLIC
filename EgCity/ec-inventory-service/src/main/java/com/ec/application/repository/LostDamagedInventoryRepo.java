package com.ec.application.repository;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.UsageLocation;

public interface LostDamagedInventoryRepo extends BaseRepository<LostDamagedInventory, Long> 
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	LostDamagedInventory save(LostDamagedInventory entity);
	
	@Query(value="SELECT count(*) from LostDamagedInventory m where m.product.productId=:productId")
	int productUsageCount(@Param("productId") Long productId);

	@Query(value="SELECT count(*) from LostDamagedInventory m where m.warehouse.warehouseName=:warehouseName")
	int warehouseUsageCount(@Param("warehouseName") String warehouseName);

	
}
