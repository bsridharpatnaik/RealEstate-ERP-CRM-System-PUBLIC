package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.LostDamagedInventory;

public interface LostDamagedInventoryRepo extends BaseRepository<LostDamagedInventory, Long> 
{

	@Query(value="SELECT count(*) from LostDamagedInventory m where m.product.productId=:productId")
	int productUsageCount(@Param("productId") Long productId);

}
