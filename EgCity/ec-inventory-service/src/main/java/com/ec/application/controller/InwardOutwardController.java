package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.EntryIdsData;
import com.ec.application.service.InwardOutwardListService;

@RestController
@RequestMapping("/iol")
public class InwardOutwardController
{

	@Autowired
	InwardOutwardListService iolService;

	@PostMapping
	public void fixClosingStock(@RequestBody EntryIdsData payload)
	{
		iolService.correctClosingStock(payload.getStartEntryId(), payload.getEndEntryId());
	}
}
