package com.ec.application.controller;

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

import com.ec.application.data.CreateLostOrDamagedInventoryData;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.service.LostDamagedInventoryService;

@RestController
@RequestMapping("/lostdamaged")
public class LostOrDamagedInventoryController 
{
	@Autowired
	LostDamagedInventoryService lostDamagedInventoryService;

	@GetMapping
	public Page<LostDamagedInventory> returnAllLostDamaged(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return lostDamagedInventoryService.findAll(pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public LostDamagedInventory createLDInventory(@RequestBody CreateLostOrDamagedInventoryData payload) throws Exception
	{
		
		return lostDamagedInventoryService.createData(payload);
	}
	
	@GetMapping("/{id}")
	public LostDamagedInventory findLDInventoryByID(@PathVariable long id) throws Exception 
	{
		return lostDamagedInventoryService.findById(id);
	}
	
	@PutMapping("/{id}")
	public LostDamagedInventory updateLDInventoryByID(@PathVariable long id, @RequestBody CreateLostOrDamagedInventoryData payload) throws Exception 
	{
		return lostDamagedInventoryService.UpdateData(payload, id);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteLDInventoryByID(@PathVariable long id) throws Exception 
	{
		lostDamagedInventoryService.DeleteData(id);
		return ResponseEntity.ok("Entity deleted");
	}
}
