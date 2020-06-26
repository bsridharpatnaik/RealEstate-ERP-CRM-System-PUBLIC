package com.ec.crm.Controller;

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

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Service.PropertyTypeService;

@RestController
@RequestMapping(value="/propertytype",produces = { "application/json", "text/json" })
public class PropertyTypeController {
	@Autowired
	PropertyTypeService propertyService;
	
	@GetMapping
	public Page<PropertyType> returnAllPropertyType(Pageable pageable) 
	{
		return propertyService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public PropertyType findPropertyTypeByID(@PathVariable long id) throws Exception 
	{
		return propertyService.findSinglePropertyType(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public PropertyType createPropertyType(@RequestBody PropertyType ptype) throws Exception{
		
		return propertyService.createPropertyType(ptype);
	}
	
	@PutMapping("/{id}")
	public PropertyType updatePropertyType(@PathVariable Long id, @RequestBody PropertyType ptype) throws Exception 
	{
		return propertyService.updatePropertyType(id, ptype);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deletePropertyType(@PathVariable Long id) throws Exception
	{
		propertyService.deletePropertyType(id);
			return ResponseEntity.ok("Source Deleted sucessfully.");
	}
}
