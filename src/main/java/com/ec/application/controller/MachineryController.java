package com.ec.application.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.BasicEntities.Machinery;
import com.ec.application.service.MachineryService;

@RestController
@RequestMapping("ec/machinery")
public class MachineryController 
{
	@Autowired
	MachineryService machineryService;
	
	@GetMapping
	public Page<Machinery> returnAllPayments(Pageable pageable) 
	{
		
		return machineryService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Machinery findMachinerybyvehicleNoMachinerys(@PathVariable long id) 
	{
		return machineryService.findSingleMachinery(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteMachinery(@PathVariable Long id) throws Exception
	{
		machineryService.deleteMachinery(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Machinery createMachinery(@RequestBody Machinery payload) throws Exception{
		
		return machineryService.createMachinery(payload);
	}

	@PutMapping("/{id}")
	public Machinery updateMachinery(@PathVariable Long id, @RequestBody Machinery Machinery) throws Exception 
	{
		return machineryService.updateMachinery(id, Machinery);
	} 
	
	@GetMapping("/name/{name}")
	public ArrayList<Machinery> returnCusByName(@PathVariable String name) 
	{
		return machineryService.findMachinerysByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<Machinery> returnCusByPartialName(@PathVariable String name) 
	{
		return machineryService.findMachinerysByPartialName(name);
	}
	
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return machineryService.findIdAndNames();
	}
}
