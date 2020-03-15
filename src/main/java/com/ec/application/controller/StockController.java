package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.model.Stock.Stock;
import com.ec.application.service.StockService;

@RestController
@RequestMapping("ec/stock")
public class StockController 
{
	@Autowired
	StockService stockService;
	
	@GetMapping
	public Page<Stock> returnAllStock(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return stockService.findStockForAll(pageable);
	}

	@GetMapping("/product/{id}") 
	public Page<Stock> findStockForProduct(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size,@PathVariable long id) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return stockService.findStockForProduct(pageable, id);
	}
	@GetMapping("/category/{id}") 
	public Page<Stock> findStockForCategory(@RequestParam(name="page",required = false) Integer page,@RequestParam(name="size",required = false) Integer size,@PathVariable long id) 
	{
		page= page==null?0:page; size = size==null?Integer.MAX_VALUE:size; 
		Pageable pageable = PageRequest.of(page, size);
		return stockService.findStockForCategory(pageable, id);
	}
	
}
