package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.OutwardInventoryData;
import com.ec.application.data.OutwardInventoryWithDropdownValues;
import com.ec.application.model.OutwardInventory;
import com.ec.application.service.OutwardInventoryService;

@RestController
@RequestMapping("ec/outward")
public class OutwardInventoryController 
{
	@Autowired
	OutwardInventoryService outwardInventoryService;
	
	@GetMapping
	public OutwardInventoryWithDropdownValues returnAllOutward(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return outwardInventoryService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public OutwardInventory createMachineryOnRent(@RequestBody OutwardInventoryData payload) throws Exception
	{
		return outwardInventoryService.createOutwardnventory(payload);
	}

}
