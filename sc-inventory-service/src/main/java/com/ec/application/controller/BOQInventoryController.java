package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.BOQRequestData;
import com.ec.application.service.BOQInventoryMappingService;
import com.ec.application.service.BuildingTypeService;
import com.ec.application.service.LocationService;

@RestController
@RequestMapping("/boq")
public class BOQInventoryController
{
	@Autowired
	BOQInventoryMappingService bimService;

	@Autowired
	BuildingTypeService btService;

	@Autowired
	LocationService lService;

	@PostMapping("/create")
	@ResponseStatus(HttpStatus.CREATED)
	public void createBuildingType(@RequestBody BOQRequestData payload) throws Exception
	{
		bimService.createNewBOQ(payload);
	}

	@GetMapping("/alltypes")
	@ResponseStatus(HttpStatus.OK)
	public List<IdNameProjections> getAllValidBuildingTypes()
	{
		return btService.findIdAndNames();
	}

	@GetMapping("/type/{id}")
	@ResponseStatus(HttpStatus.OK)
	public List<IdNameProjections> getAllUnitsUnderType(@PathVariable Long id) throws Exception
	{
		return lService.getLocationsUnderType(id);
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
