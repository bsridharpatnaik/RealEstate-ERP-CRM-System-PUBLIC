package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.MORDropdownData;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.model.BasicEntities.Product;
import com.ec.application.service.PopulateDropdownService;

@RestController
@RequestMapping("ec/dropdown")
public class PopulateDropdownController 
{

	@Autowired
	PopulateDropdownService pdService;
	
	@GetMapping("/mor")
	public NameAndProjectionDataForDropDown findProductbyvehicleNoProducts() 
	{
		return pdService.fetchData();
	}
}
