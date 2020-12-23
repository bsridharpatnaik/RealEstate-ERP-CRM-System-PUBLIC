package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.BOQInventoryMapping;
import com.ec.application.model.BuildingType;
import com.ec.application.model.UsageLocation;

@Repository
public interface BOQInventoryMappingRepo extends BaseRepository<BOQInventoryMapping, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	BOQInventoryMapping save(BuildingType entity);

	@Query(value = "SELECT m from BOQInventoryMapping m where m.buildingType=:bt AND m.product.productId=:productId")
	List<BOQInventoryMapping> findByBuildingTypeAndProduct(@Param("bt") BuildingType bt,
			@Param("productId") Long productId);

	@Query(value = "SELECT m from BOQInventoryMapping m where m.location=:ul AND m.product.productId=:productId")
	List<BOQInventoryMapping> findByLocationProduct(@Param("ul") UsageLocation ul, @Param("productId") Long productId);
}
