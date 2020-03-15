package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.BasicEntities.Contractor;
import com.ec.application.service.ContractorService;

@RestController
@RequestMapping("ec/contractor")
public class ContractorController 
{
	@Autowired
	ContractorService contractorService;
	
	@GetMapping
	public Page<Contractor> returnAllPayments(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return contractorService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Contractor findContractorbyvehicleNoContractors(@PathVariable long id) 
	{
		return contractorService.findSingleContractor(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteContractor(@PathVariable Long id) throws Exception
	{
		contractorService.deleteContractor(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Contractor createContractor(@RequestBody Contractor payload) throws Exception{
		
		return contractorService.createContractor(payload);
	}

	@PutMapping("/{id}")
	public Contractor updateContractor(@PathVariable Long id, @RequestBody Contractor Contractor) throws Exception 
	{
		return contractorService.updateContractor(id, Contractor);
	} 
	
	@GetMapping("/name/{name}")
	public ArrayList<Contractor> returnCusByName(@PathVariable String name) 
	{
		return contractorService.findContractorsByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<Contractor> returnCusByPartialName(@PathVariable String name) 
	{
		return contractorService.findContractorsByPartialName(name);
	}
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnidandNames() 
	{
		return contractorService.findIdAndNames();
	}
}
