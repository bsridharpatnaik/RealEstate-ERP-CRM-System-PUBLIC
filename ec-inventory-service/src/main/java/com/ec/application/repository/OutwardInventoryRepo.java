package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.Machinery;
import com.ec.application.model.OutwardInventory;
import com.ec.application.service.PopulateDropdownService;
import com.ec.application.service.StockService;

@Repository
public interface OutwardInventoryRepo extends BaseRepository<OutwardInventory, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	OutwardInventory save(OutwardInventory entity);
	
	@Query(value="SELECT count(m) from OutwardInventory m where m.usageLocation.locationId=:locationId")
	int locationUsageCount(@Param("locationId")Long locationId);

	@Query(value="SELECT count(m) from OutwardInventory m where m.warehouse.warehouseName=:warehouseName")
	int warehouseUsageCount(@Param("warehouseName")String warehouseName);

	@Query(value="SELECT count(m) from OutwardInventory m where m.usageArea.usageAreaId=:usageAreaId")
	int usageAreaUsageCount(@Param("usageAreaId")Long usageAreaId);
	
	@Query(value="SELECT count(*) from OutwardInventory m where m.contractor.contactId=:id")
	int contractorUsageCount(@Param("id") Long id);

	@Query(value="SELECT new com.ec.application.data.ProductGroupedDAO(iol.product.productName as productname,iol.product.measurementUnit as measurementUnit,sum(iol.quantity) as quantity) from OutwardInventory ii"
			+ " left join  ii.inwardOutwardList iol group by iol.product.productName,iol.product.measurementUnit")
	List<ProductGroupedDAO> findGroupByInfo();
}
