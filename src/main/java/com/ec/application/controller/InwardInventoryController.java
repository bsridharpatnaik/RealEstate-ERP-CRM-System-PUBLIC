package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.InwardInventoryWithDropdownValues;
import com.ec.application.data.MachineryOnRentWithDropdownData;
import com.ec.application.model.InwardInventory;
import com.ec.application.service.InwardInventoryService;

@RestController
@RequestMapping("ec/inward")
public class InwardInventoryController 
{
	@Autowired
	InwardInventoryService iiService;
	
	@GetMapping
	public InwardInventoryWithDropdownValues returnAllInward(Pageable pageable) 
	{
		
		return iiService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public InwardInventory createMachineryOnRent(@RequestBody InwardInventoryData payload) throws Exception
	{
		
		return iiService.createInwardnventory(payload);
	}
}
