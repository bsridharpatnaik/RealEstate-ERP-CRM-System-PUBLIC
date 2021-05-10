package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.AllUsageAreasWithNamesData;
import com.ec.application.model.UsageArea;
import com.ec.application.service.UsageAreaService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/usagearea")
public class UsageAreaController
{
	@Autowired
	UsageAreaService usageAreaService;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public AllUsageAreasWithNamesData returnFilteredCategories(@RequestBody FilterDataList filterDataList,
			@PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable)
	{
		return usageAreaService.findFilteredUsageAreasWithTA(filterDataList, pageable);
	}

	@GetMapping("/{id}")
	public UsageArea findUsageAreabyvehicleNoUsageAreas(@PathVariable long id)
	{
		return usageAreaService.findSingleUsageArea(id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteUsageArea(@PathVariable Long id) throws Exception
	{

		usageAreaService.deleteUsageArea(id);
		return ResponseEntity.ok("Entity deleted");
	}

	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	public UsageArea createUsageArea(@RequestBody UsageArea payload) throws Exception
	{

		return usageAreaService.createUsageArea(payload);
	}

	@PutMapping("/{id}")
	public UsageArea updateUsageArea(@PathVariable Long id, @RequestBody UsageArea UsageArea) throws Exception
	{
		return usageAreaService.updateUsageArea(id, UsageArea);
	}

	@GetMapping("/idandnames")
	public List<IdNameProjections> returnIDandNames()
	{
		return usageAreaService.findIdAndNames();
	}

	@ExceptionHandler(
	{ JpaSystemException.class })
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiOnlyMessageAndCodeError sqlError(Exception ex)
	{
		ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
				"Something went wrong while handling data. Contact Administrator.");
		return apiError;
	}
}
