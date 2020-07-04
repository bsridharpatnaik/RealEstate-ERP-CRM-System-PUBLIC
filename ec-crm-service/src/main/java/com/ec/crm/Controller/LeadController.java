package com.ec.crm.Controller;

import javax.validation.Valid;

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


import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Service.LeadService;

@RestController
@RequestMapping(value="/lead",produces = { "application/json", "text/json" })
public class LeadController {
	@Autowired
	LeadService leadService;
	
	@GetMapping
	public Page<Lead> returnAllSentiment(Pageable pageable) 
	{
		return leadService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Lead findLeadByID(@PathVariable long id) throws Exception 
	{
		return leadService.findSingleLead(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Lead createLead(@Valid @RequestBody LeadCreateData payload) throws Exception{
		
		return leadService.createLead(payload);
	}
	
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteProduct(@PathVariable Long id) throws Exception
	{
		leadService.deleteLead(id);
		return ResponseEntity.ok("Entity deleted");
	}
	
	@PutMapping("/{id}")
	public Lead updateLead(@PathVariable Long id, @RequestBody LeadCreateData payload) throws Exception 
	{
		return leadService.updateLead(id, payload);
	} 
}
