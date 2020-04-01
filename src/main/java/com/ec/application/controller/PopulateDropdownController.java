package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.service.PopulateDropdownService;

@RestController
@RequestMapping("inventory/dropdown")
public class PopulateDropdownController 
{

	@Autowired
	PopulateDropdownService pdService;
	
	@GetMapping("/{page}")
	public NameAndProjectionDataForDropDown findDropdownValuesForMOR(@PathVariable String page) 
	{
		return pdService.fetchData(page);
	}
}
