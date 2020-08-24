package com.ec.crm.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.LeadActivityClosingComment;
import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.LeadActivityOnLeadInformationDTO;
import com.ec.crm.Data.LeadActivityListWithTypeAheadData;
import com.ec.crm.Data.RescheduleActivityData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Service.LeadActivityService;
import com.ec.crm.Service.UserDetailsService;

@RestController
@RequestMapping(value="/activity",produces = { "application/json", "text/json" })
public class LeadActivityController 
{

	@Autowired
	UserDetailsService userDetailsService;
	
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
		return laService.getSingleLeadActivity(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public LeadActivity createLeadActivity(@RequestBody LeadActivityCreate at) throws Exception{
		
		return laService.createLeadActivity(at);
	}
	
	@PatchMapping("/{id}") 
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void closeLeadActivityWithComment(@RequestBody LeadActivityClosingComment payload,@PathVariable long id) throws Exception
	{
		if(payload.getClosingComment()==null)
			throw new Exception("Please enter closing comments");
		
		laService.deleteLeadActivity(id, payload.getClosingComment(),userDetailsService.getCurrentUser().getId());
	}
	
	@PutMapping("get/{id}") 
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void rescheduleActivity(@RequestBody RescheduleActivityData payload,@PathVariable long id) throws Exception
	{
		laService.rescheduleActivity(id, payload);
	}
	
	@PutMapping("revert/{id}") 
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void revertLeadActivity(@PathVariable long id) throws Exception
	{
		laService.revertLeadActivity(id);
	}
	
	@PostMapping("/getleadactivitypage") 
	@ResponseStatus(HttpStatus.OK)
	public LeadActivityListWithTypeAheadData getLeadActivityPage(@RequestBody FilterDataList leadFilterDataList,@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable) throws Exception 
	{
		return laService.getLeadActivityPage(leadFilterDataList,pageable);
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
	
	@GetMapping("/validActivityTypes")
	public List<String> findValidActivityTypes() 
	{
		return ActivityTypeEnum.getValidActivityTypes();
	}
}
