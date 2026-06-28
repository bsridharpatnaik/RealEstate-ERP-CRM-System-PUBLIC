package com.ec.application.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ec.application.data.InventoryReportByDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.AllInventoryAndInwardOutwardListProjection;
import com.ec.application.data.DashboardInwardOutwardInventoryDAO;
import com.ec.application.model.AllInventoryTransactions;

@Repository
public interface AllInventoryRepo extends BaseRepository<AllInventoryTransactions, String>
{

	// @Query(value="SELECT new
	// com.ec.application.data.DashboardOutwardInventoryDAO(date,productName,quantity,warehouseName,name)
	// from AllInventoryTransactions m")
	// List<DashboardInwardOutwardInventoryDAO> findForDashboard();

	@Query(value = "SELECT new com.ec.application.data.DashboardInwardOutwardInventoryDAO(date,productName,quantity,warehouseName,name)  "
			+ "from AllInventoryTransactions m where m.type=:type")
	List<DashboardInwardOutwardInventoryDAO> findForDashboard(@Param("type") String type, Pageable pageable);

	@Query("select m from AllInventoryTransactions m where m.entryid=:entryId")
	List<AllInventoryTransactions> findByEntryId(@Param("entryId") Long entryId);
/*
	@Query("select ai.entryid as entryid, ai.closingStock as aiClosingStock,iol.closingStock as iolClosingStock from AllInventoryTransactions ai  JOIN InwardOutwardList iol on iol.entryid=ai.entryid and iol.closingStock!=ai.closingStock")
	List<AllInventoryAndInwardOutwardListProjection> findClosingStockNotMatched();*/

	@Query(value="SELECT " +
			"t2.id," +
			"t1.month," +
			"t1.product_name," +
			"t1.measurementunit," +
			"t1.category_name," +
			"t1.warehousename," +
			"(t2.closing_stock - t1.total_inward - t1.total_transfer_in - t1.total_excess_found + t1.total_outward + t1.total_transfer_out + t1.total_lost_damaged) as opening_stock," +
			"t1.total_inward," +
			"t1.total_transfer_in," +
			"t1.total_outward," +
			"t1.total_transfer_out," +
			"t1.total_lost_damaged," +
			"t1.total_excess_found," +
			"t2.closing_stock" +
			" FROM" +
			" (SELECT" +
			" DATE_FORMAT(date,'%Y-%m') as month," +
			" category_name," +
			" product_name," +
			" measurementunit," +
			" ai1.warehousename," +
			" SUM(IF(type='Inward',quantity,0)) as total_inward," +
			" SUM(IF(type='Transfer-In',quantity,0)) as total_transfer_in," +
			" SUM(IF(type='Outward',quantity,0)) as total_outward," +
			" SUM(IF(type='Transfer-Out',quantity,0)) as total_transfer_out," +
			" SUM(IF(type='Lost-Damaged',quantity,0)) as total_lost_damaged," +
			" SUM(IF(type='Excess-Found',quantity,0)) as total_excess_found" +
			" FROM all_inventory ai1" +
			" WHERE date>=:startDate AND date<=:endDate" +
			" GROUP BY month,category_name,product_name,measurementunit,ai1.warehousename" +
			" ORDER BY month,category_name,product_name,measurementunit,ai1.warehousename" +
			") AS t1" +
			" INNER JOIN" +
			" (SELECT" +
			" DATE_FORMAT(date,'%Y-%m') as month," +
			" ai.id," +
			" ai.closingStock AS closing_stock," +
			" ai.product_name," +
			" ai.category_name," +
			" ai.warehousename" +
			" FROM all_inventory ai" +
			" INNER JOIN (" +
			" SELECT DATE_FORMAT(date,'%Y-%m') as month, product_name, category_name, warehousename," +
			" SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY date DESC, sort_order DESC, entryid DESC), ',', 1) AS id" +  // ← removed duplicate 'ai.' alias
			" FROM all_inventory" +                                                                                  // ← removed duplicate 'ai' alias
			" WHERE date>=:startDate AND date<=:endDate" +
			" GROUP BY month,product_name,category_name,warehousename" +
			" ) latest ON latest.id = ai.id" +                                                                       // ← closing paren added here
			" ) as t2 ON t1.month=t2.month AND t1.category_name=t2.category_name" +
			" AND t1.product_name=t2.product_name AND t1.warehousename=t2.warehousename" +
			" ORDER BY t1.month desc,product_name,warehousename",
			nativeQuery = true)
	ArrayList<InventoryReportByDate> getFilteredTransactionReport(
			@Param("startDate") Date startDate,
			@Param("endDate") Date endDate);

	@Query("SELECT a FROM AllInventoryTransactions a WHERE a.productId IN :productIds ORDER BY a.id")
	List<AllInventoryTransactions> findInwardOutwardByProductIds(@Param("productIds") List<Long> productIds);

	// Aging: product IDs whose oldest stock-increasing transaction date (Inward/Transfer-In/
	// Excess-Found), in a warehouse that currently still has stock, is on or before cutoffDate.
	// Per StockService.calculateStockAges' FIFO assumption, the oldest such transaction's date IS
	// the age of a warehouse's surviving stock — done in SQL instead of fetching full transaction
	// history per product and walking it in Java.
	@Query(value =
		"SELECT ai.productid FROM all_inventory ai " +
		"WHERE ai.type IN ('Inward','Transfer-In','Excess-Found') " +
		"AND EXISTS (" +
		"  SELECT 1 FROM Stock s WHERE s.productId = ai.productid AND s.warehouseId = ai.warehouse_id" +
		"  AND s.quantityInHand > 0 AND s.is_deleted = 0" +
		") " +
		"GROUP BY ai.productid " +
		"HAVING MIN(ai.date) <= :cutoffDate",
		nativeQuery = true)
	List<Long> findAgingProductIdsFifo(@Param("cutoffDate") Date cutoffDate);

	@Query(value =
		"SELECT ai.productid FROM all_inventory ai " +
		"WHERE ai.type IN ('Inward','Transfer-In','Excess-Found') " +
		"AND ai.productid IN (:ids) " +
		"AND EXISTS (" +
		"  SELECT 1 FROM Stock s WHERE s.productId = ai.productid AND s.warehouseId = ai.warehouse_id" +
		"  AND s.quantityInHand > 0 AND s.is_deleted = 0" +
		") " +
		"GROUP BY ai.productid " +
		"HAVING MIN(ai.date) <= :cutoffDate",
		nativeQuery = true)
	List<Long> findAgingProductIdsFifoIn(@Param("cutoffDate") Date cutoffDate, @Param("ids") List<Long> ids);
}