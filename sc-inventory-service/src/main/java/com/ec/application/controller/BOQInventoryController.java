package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.BOQCreateRequestData;
import com.ec.application.data.BOQUpdateRequestData;
import com.ec.application.model.BOQInventoryMapping;
import com.ec.application.model.BOQLocationTypeEnum;
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
	public void createBuildingType(@RequestBody BOQCreateRequestData payload) throws Exception
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

	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public BOQInventoryMapping getSingleBOQ(@PathVariable Long id) throws Exception
	{
		return bimService.getOne(id);
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public BOQInventoryMapping updateBOQ(@PathVariable Long id, @RequestBody BOQUpdateRequestData payload)
			throws Exception
	{
		return bimService.updateBOQ(payload, id);
	}

	@GetMapping("/bytype/{id}")
	@ResponseStatus(HttpStatus.OK)
	public Page<BOQInventoryMapping> getBOQByType(@PathVariable Long id,
			@PageableDefault(page = 0, size = 10, sort = "lastModifiedDate", direction = Direction.DESC) Pageable pageable)
			throws Exception
	{
		return bimService.getBOQByType(id, pageable);
	}

	@GetMapping("/byunit/{id}")
	@ResponseStatus(HttpStatus.OK)
	public Page<BOQInventoryMapping> getBOQByLocation(@PathVariable Long id,
			@PageableDefault(page = 0, size = 10, sort = "lastModifiedDate", direction = Direction.DESC) Pageable pageable)
			throws Exception
	{
		return bimService.getBOQByLocation(id, pageable);
	}

	@GetMapping("/getproductlist/{id}")
	public List<IdNameProjections> getProductList(@PathVariable Long id, @RequestParam BOQLocationTypeEnum boqtype)
			throws Exception
	{
		if (boqtype == null)
			throw new Exception("Required Parameter boqtype not found");

		if (id == null)
			throw new Exception("ID cannot be null");

		return bimService.getProductListForDropdown(boqtype, id);
	}

	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteBOQ(@PathVariable Long id) throws Exception
	{
		bimService.deleteBOQEntry(id);
		return ResponseEntity.ok("Entity deleted");
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
