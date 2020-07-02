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

import com.ec.crm.Model.Broker;
import com.ec.crm.Service.BrokerService;

@RestController
@RequestMapping(value="/broker",produces = { "application/json", "text/json" })
public class BrokerController {
	@Autowired
	BrokerService brokerService;
	
	@GetMapping
	public Page<Broker> returnAllBroker(Pageable pageable) 
	{
		return brokerService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public Broker findBrokerByID(@PathVariable long id) throws Exception 
	{
		return brokerService.findSingleBroker(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Broker createBroker(@RequestBody Broker broker) throws Exception{
		
		return brokerService.createBroker(broker);
	}
	
	@PutMapping("/{id}")
	public Broker updateBroker(@PathVariable Long id, @RequestBody Broker broker) throws Exception 
	{
		return brokerService.updateBroker(id, broker);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteBroker(@PathVariable Long id) throws Exception
	{
		brokerService.deleteBroker(id);
			return ResponseEntity.ok("Broker Deleted sucessfully.");
	}
}
