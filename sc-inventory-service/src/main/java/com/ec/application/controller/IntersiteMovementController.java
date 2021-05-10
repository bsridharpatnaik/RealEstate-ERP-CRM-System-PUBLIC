package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.SingleStringData;
import com.ec.application.model.Warehouse;
import com.ec.application.service.IntersiteMovementService;

@RestController
@RequestMapping("/ism")
public class IntersiteMovementController
{

	@Autowired
	IntersiteMovementService ismService;

	@PostMapping("/warehouses")
	@ResponseStatus(HttpStatus.OK)
	public List<Warehouse> getAllWarehouses(@RequestBody SingleStringData data) throws Exception
	{
		return ismService.getAllWarehouses(data);
	}

}
