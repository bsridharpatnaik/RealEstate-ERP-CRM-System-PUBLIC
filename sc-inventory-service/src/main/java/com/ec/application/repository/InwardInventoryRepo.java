package com.ec.application.repository;


import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.model.InventoryNotification;
import com.ec.application.model.InwardInventory;

@Repository
public interface InwardInventoryRepo extends BaseRepository<InwardInventory, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	InwardInventory save(InwardInventory entity);
	
	@Query(value="SELECT count(*) from InwardInventory m where m.warehouse.warehouseName=:warehouseName")
	int warehouseUsageCount(@Param("warehouseName") String warehouseName);

	@Query(value="SELECT count(*) from InwardInventory m where m.supplier.contactId=:id")
	int supplierUsageCount(@Param("id") Long id);

	@Query(value="SELECT new com.ec.application.data.ProductGroupedDAO(iol.product.productName as productname,iol.product.measurementUnit as measurementUnit,sum(iol.quantity) as quantity) from InwardInventory ii"
			+ " left join  ii.inwardOutwardList iol group by iol.product.productName,iol.product.measurementUnit")
	List<ProductGroupedDAO> findGroupByInfo();

	//year(e.eventDate) = ?1 and month(e.eventDate) = ?2"
	@Query(value="SELECT i from InwardInventory i WHERE year(i.date)=year(current_date) AND month(date)=month(current_date)")
    List<InwardInventory> getCurrentMonthData();
}
