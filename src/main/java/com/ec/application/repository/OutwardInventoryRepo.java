package com.ec.application.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.OutwardInventoryData;
import com.ec.application.model.OutwardInventory;
import com.ec.application.service.PopulateDropdownService;
import com.ec.application.service.StockService;

@Repository
public interface OutwardInventoryRepo extends BaseRepository<OutwardInventory, Long>
{
	
}
