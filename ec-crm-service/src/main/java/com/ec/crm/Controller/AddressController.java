package com.ec.crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Service.AddressService;
import com.ec.crm.Model.Address;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping(value="/address",produces = { "application/json", "text/json" })
public class AddressController {
	
	@Autowired
	AddressService addressService;
	
	@GetMapping
	public Page<Address> returnAllAddress(Pageable pageable) 
	{
		return addressService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Address findAddressByID(@PathVariable long id) throws Exception 
	{
		return addressService.findSingleAddress(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Address createAddress(@RequestBody Address address) throws Exception{
		
		return addressService.createAddress(address);
	}
	
	@PutMapping("/{id}")
	public Address updateAddress(@PathVariable Long id, @RequestBody Address address) throws Exception 
	{
		return addressService.updateAddress(id, address);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteAddress(@PathVariable Long id) throws Exception
	{
			addressService.deleteAddress(id);
			return ResponseEntity.ok("Address Deleted sucessfully.");
	}
}
