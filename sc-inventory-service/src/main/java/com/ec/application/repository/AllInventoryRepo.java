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
			"    t1.month," +
			"t1.product_name," +
			"    t1.measurementunit," +
			"    t1.category_name," +
			"    t1.warehousename," +
			"    IF(total_inward-total_outward-total_lost_damaged=closing_stock,0,(t2.closing_stock+total_outward+total_lost_damaged-total_inward)) as opening_stock," +
			"    t1.total_inward," +
			"    total_outward," +
			"    total_lost_damaged," +
			"    t2.closing_stock" +
			" FROM" +
			" (" +
			"SELECT" +
			" DATE_FORMAT(date,'%Y-%m') as month," +
			" category_name," +
			" product_name," +
			"        measurementunit," +
			"        ai1.warehousename," +
			"SUM(IF(type='Inward',quantity,0)) as total_inward," +
			"SUM(IF(type='Outward',quantity,0)) as total_outward," +
			"SUM(IF(type='Lost-Damaged',quantity,0)) as total_lost_damaged" +
			" FROM all_inventory ai1" +
			"    WHERE date>=:startDate AND date<=:endDate" +
			" GROUP BY month,category_name,product_name,measurementunit,ai1.warehousename" +
			" ORDER BY month,category_name,product_name,measurementunit, ai1.warehousename" +
			") AS t1" +
			" INNER JOIN" +
			" (" +
			" SELECT" +
			"    DATE_FORMAT(date,'%Y-%m') as month," +
			" ai.id," +
			" ai.closingStock AS closing_stock," +
			"    ai.product_name," +
			" ai.category_name," +
			" ai.warehousename" +
			" FROM all_inventory ai" +
			" INNER JOIN (" +
			" SELECT" +
			" DATE_FORMAT(date,'%Y-%m') as month," +
			"product_name," +
			"category_name," +
			"warehousename," +
			"MIN(id) AS id" +
			" FROM all_inventory" +
			"        WHERE date>=:startDate AND date<=:endDate" +
			"        GROUP BY month,product_name,category_name,warehousename" +
			" ) latest ON latest.id = ai.id" +
			" ) as t2 ON t1.month=t2.month AND t1.category_name=t2.category_name AND t1.product_name=t2.product_name AND t1.warehousename=t2.warehousename" +
			" ORDER BY t1.month desc,product_name,warehousename",nativeQuery = true)
	ArrayList<InventoryReportByDate> getFilteredTransactionReport(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}