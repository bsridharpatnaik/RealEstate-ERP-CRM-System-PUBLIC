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
import com.ec.application.model.BasicEntities.Vendor;
import com.ec.application.service.VendorService;

@RestController
@RequestMapping("ec/vendor")
public class VendorController 
{
	@Autowired
	VendorService VendorService;
	
	@GetMapping
	public Page<Vendor> returnAllPayments(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return VendorService.findAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Vendor findVendorbyvehicleNoVendors(@PathVariable long id) 
	{
		return VendorService.findSingleVendor(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteVendor(@PathVariable Long id) throws Exception
	{
		VendorService.deleteVendor(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Vendor createVendor(@RequestBody Vendor payload) throws Exception{
		
		return VendorService.createVendor(payload);
	}

	@PutMapping("/{id}")
	public Vendor updateVendor(@PathVariable Long id, @RequestBody Vendor Vendor) throws Exception 
	{
		return VendorService.updateVendor(id, Vendor);
	} 
	
	@GetMapping("/name/{name}")
	public ArrayList<Vendor> returnCusByName(@PathVariable String name) 
	{
		return VendorService.findVendorsByName(name);
	}
	@GetMapping("/partialname/{name}")
	public ArrayList<Vendor> returnCusByPartialName(@PathVariable String name) 
	{
		return VendorService.findVendorsByPartialName(name);
	}
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return VendorService.findIdAndNames();
	}
}
