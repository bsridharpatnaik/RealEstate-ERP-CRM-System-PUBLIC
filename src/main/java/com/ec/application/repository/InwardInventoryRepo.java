package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.InwardInventory;

@Repository
public interface InwardInventoryRepo extends BaseRepository<InwardInventory, Long>
{
	@Query(value="SELECT count(*) from InwardInventory m where m.product.productId=:productId")
	int productUsageCount(Long productId);

	@Query(value="SELECT count(*) from InwardInventory m where m.unloadingArea.unloadingAreaId=:unloadingAreaId")
	int unloadingAreaCount(Long unloadingAreaId);

	@Query(value="SELECT count(*) from InwardInventory m where m.vendor.vendorId=:vendorId")
	int vendorCount(Long vendorId);
}
