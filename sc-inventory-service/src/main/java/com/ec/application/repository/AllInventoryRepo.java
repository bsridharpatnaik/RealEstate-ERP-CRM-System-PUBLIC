package com.ec.application.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
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

	@Query("select ai.entryid as entryid, ai.closingStock as aiClosingStock,iol.closingStock as iolClosingStock from AllInventoryTransactions ai  JOIN InwardOutwardList iol on iol.entryid=ai.entryid and iol.closingStock!=ai.closingStock")
	List<AllInventoryAndInwardOutwardListProjection> findClosingStockNotMatched();
}