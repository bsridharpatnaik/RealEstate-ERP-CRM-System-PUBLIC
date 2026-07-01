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

	@Query("    SELECT COUNT(DISTINCT m)    FROM InwardInventory m    JOIN m.inwardOutwardList l    WHERE l.warehouse.warehouseName = :warehouseName")
	int warehouseUsageCount(@Param("warehouseName") String warehouseName);

	@Query(value="SELECT count(*) from InwardInventory m where m.supplier.contactId=:id")
	int supplierUsageCount(@Param("id") Long id);

	@Query(value="SELECT new com.ec.application.data.ProductGroupedDAO(iol.product.productName as productname,iol.product.measurementUnit as measurementUnit,sum(iol.quantity) as quantity) from InwardInventory ii"
			+ " left join  ii.inwardOutwardList iol group by iol.product.productName,iol.product.measurementUnit")
	List<ProductGroupedDAO> findGroupByInfo();

	//year(e.eventDate) = ?1 and month(e.eventDate) = ?2"
	@Query(value="SELECT i from InwardInventory i WHERE year(i.date)=year(current_date) AND month(date)=month(current_date)")
    List<InwardInventory> getCurrentMonthData();

	/**
	 * Checks if an opening stock inward already exists for a given
	 * product + warehouse combination (identified by supplier name = 'OPENING STOCK')
	 */
	@Query("SELECT COUNT(ii) FROM InwardInventory ii " +
			"JOIN ii.inwardOutwardList iol " +
			"WHERE UPPER(ii.supplier.name) = 'OPENING STOCK' " +
			"AND iol.product.productName = :productName " +
			"AND iol.warehouse.warehouseName = :warehouseName")
	int countOpeningStockForProductAndWarehouse(
			@Param("productName") String productName,
			@Param("warehouseName") String warehouseName);

	// ── Tile counts ───────────────────────────────────────────────────────────

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE i.date >= :from")
	long countSince(@Param("from") java.util.Date from);

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE " +
			"(i.challanNo IS NULL OR TRIM(i.challanNo) = '') AND " +
			"(i.billNo IS NULL OR TRIM(i.billNo) = '')")
	long countMissingChallanBill();

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE SIZE(i.rejectInwardList) > 0")
	long countWithReject();

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE i.createdFromPO = true")
	long countFromPO();

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE " +
			"(i.createdFromPO IS NULL OR i.createdFromPO = false) AND " +
			"(i.isSampleInward IS NULL OR i.isSampleInward = false)")
	long countDirect();

	@Query("SELECT COUNT(i) FROM InwardInventory i WHERE i.isSampleInward = true")
	long countSample();
}
