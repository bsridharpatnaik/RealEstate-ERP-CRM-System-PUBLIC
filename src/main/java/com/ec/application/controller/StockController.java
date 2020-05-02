package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ProductIdAndStockProjection;
import com.ec.application.data.CurrentStockRequest;
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
	
	@PostMapping("/current")
	public List<ProductIdAndStockProjection> returnStockForProductWarehouse(@RequestBody CurrentStockRequest currentStockRequest) 
	{
		return stockService.findStockForProductListWarehouse(currentStockRequest);
	}
	
	@GetMapping("/current")
	public Float getStockForProductWarehouse(@RequestParam Long productId, @RequestParam Long warehouseId) 
	{
		Float currentStock;
		currentStock = stockService.findStockForProductWarehouse(productId,warehouseId);
		return currentStock==null?0:currentStock;
	}
}

