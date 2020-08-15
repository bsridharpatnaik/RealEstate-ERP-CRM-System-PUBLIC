package com.ec.crm.Controller;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.PlannerAllReturnDAO;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Service.AllActivitiesService;

@RestController
@RequestMapping(value="/planner/activities",produces = { "application/json", "text/json" })
public class PlannerController 
{
	@Autowired
	AllActivitiesService allActivitiesService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public PlannerAllReturnDAO returnFilteredLeadActivities(@RequestBody FilterDataList leadFilterDataList,@PageableDefault(page = 0, size = 10, sort = "leadActivityId", direction = Direction.DESC) Pageable pageable) throws ParseException 
	{
		return allActivitiesService.findFilteredDataForPlanner(leadFilterDataList,pageable);
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
