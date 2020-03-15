package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.InwardInventoryWithDropdownValues;
import com.ec.application.model.InwardInventory;
import com.ec.application.service.InwardInventoryService;

@RestController
@RequestMapping("ec/inward")
public class InwardInventoryController 
{
	@Autowired
	InwardInventoryService iiService;
	
	@GetMapping
	public InwardInventoryWithDropdownValues returnAllInward(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return iiService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public InwardInventory createInwardInventory(@RequestBody InwardInventoryData payload) throws Exception
	{
		
		return iiService.createInwardnventory(payload);
	}
	
	@GetMapping("/{id}")
	public InwardInventory getInwardInventory(@PathVariable Long id) throws Exception
	{
		
		return iiService.findInwardnventory(id);
	}
	@PutMapping("/{id}")
	public InwardInventory updateInwardInventory(@RequestBody InwardInventoryData payload,@PathVariable Long id) throws Exception
	{
		
		return iiService.updateInwardnventory(payload, id);
	}
}
