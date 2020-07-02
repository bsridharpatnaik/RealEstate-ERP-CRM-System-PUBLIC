package com.ec.crm.Controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/",produces = { "application/json", "text/json" })
public class TestController {
	@GetMapping
	public String helloworld() 
	{
		return "helloworld";
	}
}
