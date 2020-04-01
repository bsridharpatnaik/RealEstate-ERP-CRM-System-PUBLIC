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
import com.ec.application.model.BasicEntities.UsageLocation;
import com.ec.application.service.LocationService;

@RestController
@RequestMapping("inventory/location")
public class LocationController 
{

	@Autowired
	LocationService LocationService;
	
	@GetMapping
	public Page<UsageLocation> returnAllPayments(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return LocationService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public UsageLocation findLocationbyvehicleNoLocations(@PathVariable long id) 
	{
		return LocationService.findSingleLocation(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteLocation(@PathVariable Long id) throws Exception
	{
		LocationService.deleteLocation(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public UsageLocation createLocation(@RequestBody UsageLocation payload) throws Exception{
		
		return LocationService.createLocation(payload);
	}

	@PutMapping("/{id}")
	public UsageLocation updateLocation(@PathVariable Long id, @RequestBody UsageLocation Location) throws Exception 
	{
		return LocationService.updateLocation(id, Location);
	} 
	
	@GetMapping("/name/{name}")
	public ArrayList<UsageLocation> returnCusByName(@PathVariable String name) 
	{
		return LocationService.findLocationsByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<UsageLocation> returnCusByPartialName(@PathVariable String name) 
	{
		return LocationService.findLocationsByPartialName(name);
	}
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return LocationService.findIdAndNames();
	}
}
