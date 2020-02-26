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

import com.ec.application.data.CreateMORentData;
import com.ec.application.model.Category;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.service.MachineryOnRentService;

@RestController
@RequestMapping("ec/mor")
public class MachineryOnRentController 
{

	@Autowired
	MachineryOnRentService morService;
	
	@GetMapping
	public Page<MachineryOnRent> returnAllMor(Pageable pageable) 
	{
		
		return morService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public MachineryOnRent createMachineryOnRent(@RequestBody CreateMORentData payload) throws Exception{
		
		return morService.createData(payload);
	}
	
}
