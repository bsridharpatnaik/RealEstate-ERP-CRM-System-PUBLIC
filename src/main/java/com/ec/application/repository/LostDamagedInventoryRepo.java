package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.LostDamagedInventory;

public interface LostDamagedInventoryRepo extends BaseRepository<LostDamagedInventory, Long> 
{

	@Query(value="SELECT count(*) from LostDamagedInventory m where m.product.productId=:productId")
	int productUsageCount(Long productId);

}
