package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.service.AllInventoryService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/inventory")
public class AllInventoryController 
{
	@Autowired
	AllInventoryService allInventoryService;

	@PostMapping 
	@ResponseStatus(HttpStatus.OK)
	public Page<AllInventoryTransactions> fetchAllInwardInventory(@RequestBody FilterDataList filterDataList,@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) throws Exception
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return allInventoryService.fetchAllInventory(filterDataList,pageable);
	}
}
