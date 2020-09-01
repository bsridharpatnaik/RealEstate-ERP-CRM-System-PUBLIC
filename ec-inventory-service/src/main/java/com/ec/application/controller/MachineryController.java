package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.AllMachineriesWithNamesData;
import com.ec.application.model.Machinery;
import com.ec.application.service.MachineryService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/machinery")
public class MachineryController 
{
	@Autowired
	MachineryService machineryService;
	
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public AllMachineriesWithNamesData returnFilteredMachineries(@RequestBody FilterDataList filterDataList,@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable)
	{
		return machineryService.findFilteredMachineriesWithTA(filterDataList,pageable);
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
	
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return machineryService.findIdAndNames();
	}
	
	@ExceptionHandler({JpaSystemException.class})
	public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
		ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,"Something went wrong while handling data. Contact admisitrator.");
		return apiError;
	}
}
