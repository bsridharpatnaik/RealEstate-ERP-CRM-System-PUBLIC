package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	public Page<Contractor> returnAllContractors(@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable)
	{
		return contractorService.findAll(pageable);
	}
	
	@GetMapping("/names")
	public List<IdNameProjections> returnContractorNames() 
	{
		return contractorService.getContractorNames();
	}
	
	@GetMapping("/isused/{id}")
	public Boolean returnContractorIsUsed(@PathVariable Long id) 
	{
		return contractorService.isContactUsedAsContractor(id);
	}
}
