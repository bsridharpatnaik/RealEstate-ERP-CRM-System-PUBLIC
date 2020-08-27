package com.ec.crm.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.DashboardData;
import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Service.AllActivitiesService;
import com.ec.crm.Service.DashboardService;
import com.ec.crm.Model.LeadActivity;

@RestController
@RequestMapping(value="/dashboard",produces = { "application/json", "text/json" })
public class DashboardController {
	
	@Autowired
	DashboardService dashboardService;
	
	
	@PostMapping("/customerpipeline")
	@ResponseStatus(HttpStatus.OK)
	public PipelineAndActivitiesForDashboard customerpipeline(@RequestBody DashboardData payload) throws ParseException
	{
		
		return dashboardService.customerpipeline(payload);
	}
	
	@GetMapping("/stagnant")
	@ResponseStatus(HttpStatus.OK)
	public List<StagnantStats> returnStagnantStats()
	{
		return dashboardService.returnStagnantStats();
	}
	@GetMapping("/conversionratio")
	@ResponseStatus(HttpStatus.OK)
	public List<ConversionRatio> conversionratio() throws ParseException
	{	
		return dashboardService.conversionratio();
	}
	
	@GetMapping("/topperformer")
	@ResponseStatus(HttpStatus.OK) 
	public Map topperformer() throws ParseException {
	  
		return dashboardService.topperformer();
	
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
