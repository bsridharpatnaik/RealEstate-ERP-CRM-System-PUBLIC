package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
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

	@Query(value = "SELECT m from BOQInventoryMapping m where m.buildingType.typeId=:typeId order by m.lastModifiedDate")
	Page<BOQInventoryMapping> getBIMbyType(@Param("typeId") Long typeId, Pageable pageable);

	@Query(value = "SELECT m from BOQInventoryMapping m where m.location.locationId=:locationId order by m.lastModifiedDate")
	Page<BOQInventoryMapping> getBIMbyLocation(@Param("locationId") Long locationId, Pageable pageable);

	@Query(value = "SELECT bim.product.productId as id,bim.product.productName as name from BOQInventoryMapping bim WHERE bim.buildingType.typeId=:typeId")
	List<IdNameProjections> findUsedProductListForType(@Param("typeId") Long typeId);

	@Query(value = "SELECT bim.product.productId as id,bim.product.productName as name from BOQInventoryMapping bim WHERE bim.location.locationId=:locationId")
	List<IdNameProjections> findUsedProductListForUnit(@Param("locationId") Long locationId);

	@Query(value = "SELECT count(bim) from BOQInventoryMapping bim WHERE bim.location.locationId=:locationId")
	int locationUsageCount(Long locationId);

	@Query(value = "SELECT count(bim) from BOQInventoryMapping bim WHERE bim.buildingType.typeId=:typeId")
	int buildingTypeUsageCount(@Param("typeId") Long typeId);

	@Query(value = "SELECT count(bim) from BOQInventoryMapping bim WHERE bim.product.productId=:productId")
	int productUsageCount(@Param("productId") Long productId);

}
