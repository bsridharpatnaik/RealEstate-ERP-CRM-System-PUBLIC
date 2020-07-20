package com.ec.crm.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.ReusableClasses.IdNameProjections;
import com.ec.crm.Data.BrokerListWithTypeAheadData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.FilterDataList;
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
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public BrokerListWithTypeAheadData returnFilteredSource(@RequestBody FilterDataList brokerFilterDataList,@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable) 
	{
		return brokerService.findFilteredBroker(brokerFilterDataList,pageable);
	}
	@GetMapping("/{id}")
	public Broker findBrokerByID(@PathVariable long id) throws Exception 
	{
		return brokerService.findSingleBroker(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Broker createBroker(@Valid @RequestBody Broker broker) throws Exception{
		
		return brokerService.createBroker(broker);
	}
	
	@PutMapping("/{id}")
	public Broker updateBroker(@PathVariable Long id,@Valid @RequestBody Broker broker) throws Exception 
	{
		return brokerService.updateBroker(id, broker);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteBroker(@PathVariable Long id) throws Exception
	{
		brokerService.deleteBroker(id);
			return ResponseEntity.ok("Broker Deleted sucessfully.");
	}
	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIdAndNames() 
	{
		return brokerService.findIdAndNames();
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Map<String, String> handleValidationExceptions(
	  MethodArgumentNotValidException ex) {
	    Map<String, String> errors = new HashMap<>();
	    ex.getBindingResult().getAllErrors().forEach((error) -> {
	        String fieldName = ((FieldError) error).getField();
	        String errorMessage = error.getDefaultMessage();
	        errors.put(fieldName, errorMessage);
	    });
	    return errors;
	}
}
