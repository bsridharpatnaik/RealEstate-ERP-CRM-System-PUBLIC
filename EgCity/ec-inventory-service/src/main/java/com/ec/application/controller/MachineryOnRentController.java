package com.ec.application.controller;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.data.CreateMORentData;
import com.ec.application.data.MachineryOnRentWithDropdownData;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.service.MachineryOnRentService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/mor")
public class MachineryOnRentController 
{

	@Autowired
	MachineryOnRentService morService;
	
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public MachineryOnRentWithDropdownData returnAllMor(@RequestBody FilterDataList filterDataList,@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable) throws ParseException
	{
		return morService.findAllWithDropdown(filterDataList,pageable);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public MachineryOnRent createMachineryOnRent(@RequestBody CreateMORentData payload) throws Exception{
		
		return morService.createData(payload);
	}
	
	@PutMapping("/{id}")
	public MachineryOnRent updateMOR(@PathVariable Long id, @RequestBody CreateMORentData payload) throws Exception 
	{
		return morService.UpdateData(payload, id);
	} 
	
	@GetMapping("/{id}")
	public MachineryOnRent findMORbyvehicleNoMORs(@PathVariable long id) throws Exception 
	{
		return morService.findById(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteMOR(@PathVariable Long id) throws Exception
	{
		morService.DeleteData(id);
		return ResponseEntity.ok("Entity deleted");
	}
	@ExceptionHandler({JpaSystemException.class})
	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
		ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,"Something went wrong while handling data. Contact Administrator.");
		return apiError;
	}
}
