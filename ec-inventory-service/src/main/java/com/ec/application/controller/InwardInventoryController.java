package com.ec.application.controller;

import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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

import com.ec.application.data.InwardInventoryData;
import com.ec.application.data.ReturnInwardInventoryData;
import com.ec.application.model.InwardInventory;
import com.ec.application.service.InwardInventoryService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/inward")
public class InwardInventoryController 
{
	@Autowired
	InwardInventoryService iiService;
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public InwardInventory createInwardInventory(@RequestBody InwardInventoryData payload) throws Exception
	{
		
		return iiService.createInwardnventory(payload);
	}
	
	
	@PostMapping 
	@ResponseStatus(HttpStatus.OK)
	public ReturnInwardInventoryData fetchAllInwardInventory(@RequestBody FilterDataList filterDataList,
			@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable) throws Exception
	{
		
		return iiService.fetchInwardnventory(filterDataList,pageable);
	}
	
	@GetMapping("/{id}")
	public InwardInventory findInwardInventoryById(@PathVariable long id) throws Exception 
	{
		return iiService.findById(id);
	}
	
	@PutMapping("/{id}")
	public InwardInventory updateInwardInventoryById(@PathVariable long id,@RequestBody InwardInventoryData payload) throws Exception 
	{
		return iiService.updateInwardnventory(payload, id);
	}
	
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteInwardInventoryById(@PathVariable Long id) throws Exception
	{
		
		iiService.deleteInwardInventoryById(id);
		return ResponseEntity.ok("Entity deleted");
	}
}
