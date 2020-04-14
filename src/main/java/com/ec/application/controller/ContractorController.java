package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Contractor;
import com.ec.application.service.ContractorService;

@RestController
@RequestMapping("/contractor")
public class ContractorController 
{
	@Autowired
	ContractorService contractorService;
	
	@GetMapping
	public Page<Contractor> returnAllContractors(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return contractorService.findAll(pageable);
	}
	
	@GetMapping("/names")
	public List<IdNameProjections> returnContractorNames() 
	{
		return contractorService.getContractorNames();
	}
	
	
}
