package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.BOQStatusLocationsForType;
import com.ec.application.data.BOQStatusTypeListWithConsumedUnitCount;
import com.ec.application.model.BOQStatus;
import com.ec.application.service.BOQStatusService;

@RestController
@RequestMapping("/boqstatus")
public class BOQStatusController
{
	@Autowired
	BOQStatusService boqService;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<BOQStatus> fetchAllInwardInventory()
	{
		return boqService.fetchAllBOQRecord();
	}

	@GetMapping("/typelistwithcount")
	@ResponseStatus(HttpStatus.OK)
	public List<BOQStatusTypeListWithConsumedUnitCount> fetchBOQStatusTypeListWithConsumedUnitCount()
	{
		return boqService.fetchBOQStatusTypeListWithConsumedUnitCount();
	}

	@GetMapping("/locationlist/{id}")
	@ResponseStatus(HttpStatus.OK)
	public List<BOQStatusLocationsForType> getLocationWiseStatusForType(@PathVariable Long id)
	{
		return boqService.getLocationWiseStatusForType(id);
	}
}
