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

import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.ActivityType;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.Source;
import com.ec.crm.ReusableClasses.IdNameProjections;
import com.ec.crm.Service.ActivityTypeService;
import com.ec.crm.Service.LeadActivityService;
import com.ec.crm.Service.SourceService;

@RestController
@RequestMapping(value="/leadactivity",produces = { "application/json", "text/json" })
public class LeadActivityController {
	@Autowired
	LeadActivityService laService;
	
	@GetMapping
	public Page<LeadActivity> returnAllLeadActivity(Pageable pageable) 
	{
		return laService.fetchAll(pageable);
	}
	@GetMapping("/{id}")
	public LeadActivity findLeadActivityByID(@PathVariable long id) throws Exception 
	{
		return laService.findSingleLeadActivity(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public LeadActivity createLeadActivity(@Valid @RequestBody LeadActivityCreate at) throws Exception{
		
		return laService.createLeadActivity(at);
	}
	
	@PutMapping("/{id}")
	public LeadActivity updateLeadActivity(@PathVariable Long id,@Valid @RequestBody LeadActivityCreate at) throws Exception 
	{
		return laService.updateLeadActivity(id, at);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteLeadActivity(@PathVariable Long id) throws Exception
	{
		laService.deleteLeadActivity(id);
			return ResponseEntity.ok("LeadActivity Deleted sucessfully.");
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
