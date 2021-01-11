package com.ec.crm.Controller;

import java.util.HashMap;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Service.AllActivitiesService;
import com.ec.crm.Service.UserDetailsService;

@RestController
@RequestMapping(value = "/pipeline/activities", produces =
{ "application/json", "text/json" })
public class PipelineController
{
	@Autowired
	AllActivitiesService allActivitiesService;

	@Autowired
	HttpServletRequest request;

	@Autowired
	UserDetailsService userDetailsService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public PipelineAllReturnDAO returnFilteredLeadActivities(@RequestBody FilterDataList leadFilterDataList,
			@PageableDefault(page = 0, size = 10, sort = "leadActivityId", direction = Direction.DESC) Pageable pageable)
			throws Exception
	{
		UserReturnData currentUser = userDetailsService.getCurrentUser();
		request.setAttribute("currentUser", currentUser);
		return allActivitiesService.findFilteredDataForPipeline(leadFilterDataList, pageable);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex)
	{
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) ->
		{
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return errors;
	}

}
