package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.repository.AllInventoryRepo;
import com.ec.common.Filters.FilterDataList;

@Service
public class AllInventoryService 
{
	@Autowired
	AllInventoryRepo allInventoryRepo;
	
	public Page<AllInventoryTransactions> findAll(Pageable pageable) 
	{
		return allInventoryRepo.findAll(pageable);
	}

	public Page<AllInventoryTransactions> fetchAllInventory(FilterDataList filterDataList, Pageable pageable) 
	{
		return allInventoryRepo.findAll(pageable);
	}
}
