package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.EmailHelper;
import com.ec.application.service.StockService;

@RestController
@RequestMapping(value="/email",produces = { "application/json", "text/json" })
public class AutomaticEmailController 
{
	@Autowired
	EmailHelper emailHelper;
	
	@Autowired
	StockService stockService;
	
	@GetMapping
	public void sendEmail() throws Exception
	{
		stockService.sendStockNotificationEmail();
	}
}
