package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.StockInformation;
import com.ec.application.service.StockService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/stock")
public class StockController 
{
	@Autowired
	StockService stockService;
	
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public StockInformation returnAllStock(@RequestBody FilterDataList filterDataList,@RequestParam(name = "fields", required = false) String fields) 
	{
		return stockService.findStockForAll(filterDataList);
	}
}

