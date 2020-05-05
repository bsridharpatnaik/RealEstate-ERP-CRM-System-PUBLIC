package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.AllInventoryReturnData;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.common.Filters.FilterDataList;

@Service
public class AllInventoryService 
{
	@Autowired
	AllInventoryRepo allInventoryRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	
	public AllInventoryReturnData fetchAllInventory(FilterDataList filterDataList, Pageable pageable) 
	{
		AllInventoryReturnData allInventoryReturnData = new AllInventoryReturnData();
		allInventoryReturnData.setTransactions(allInventoryRepo.findAll(pageable));
		allInventoryReturnData.setLdDropdown(populateDropdownService.fetchData("allinventory"));
		return allInventoryReturnData;
	}
}
