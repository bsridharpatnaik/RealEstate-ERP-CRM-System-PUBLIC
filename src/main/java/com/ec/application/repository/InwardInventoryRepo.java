package com.ec.application.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardInventory;

@Repository
public interface InwardInventoryRepo extends BaseRepository<InwardInventory, Long>
{
	@Query(value="SELECT count(*) from InwardInventory m where m.product.productId=:productId")
	int productUsageCount(@Param("productId")Long productId);

	@Query(value="SELECT count(*) from InwardInventory m where m.warehouse.warehouseName=:warehouseName")
	int unloadingAreaCount(@Param("warehouseName") String warehouseName);
}
