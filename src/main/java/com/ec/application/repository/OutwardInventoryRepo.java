package com.ec.application.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.OutwardInventoryCreateData;
import com.ec.application.model.OutwardInventory;
import com.ec.application.service.PopulateDropdownService;
import com.ec.application.service.StockService;

@Repository
public interface OutwardInventoryRepo extends BaseRepository<OutwardInventory, Long>
{
	@Query(value="SELECT count(m) from OutwardInventory m where m.usageLocation.locationId=:locationId")
	int locationUsageCount(@Param("locationId")Long locationId);
}
