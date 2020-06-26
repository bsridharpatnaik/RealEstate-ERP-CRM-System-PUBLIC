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

import com.ec.crm.Model.Source;
import com.ec.crm.Service.SourceService;

@RestController
@RequestMapping(value="/source",produces = { "application/json", "text/json" })
public class SourceController {
	@Autowired
	SourceService sourceService;
	
	@GetMapping
	public Page<Source> returnAllBroker(Pageable pageable) 
	{
		return sourceService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Source findAddressByID(@PathVariable long id) throws Exception 
	{
		return sourceService.findSingleSource(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Source createUser(@RequestBody Source source) throws Exception{
		
		return sourceService.createSource(source);
	}
	
	@PutMapping("/{id}")
	public Source updateSource(@PathVariable Long id, @RequestBody Source source) throws Exception 
	{
		return sourceService.updateSource(id, source);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteSource(@PathVariable Long id) throws Exception
	{
		sourceService.deleteSource(id);
			return ResponseEntity.ok("Source Deleted sucessfully.");
	}
}
