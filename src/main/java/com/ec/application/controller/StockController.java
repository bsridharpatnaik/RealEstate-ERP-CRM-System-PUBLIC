package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
	public StockInformation returnAllStock(@RequestBody FilterDataList filterDataList,
			@RequestParam(name = "fields", required = false,defaultValue="stockInformation{productId}") String fields
			,@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return stockService.findStockForAll(filterDataList,pageable);
	}
}

