package com.ec.crm.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ec.crm.ReusableClasses.ReusableMethods;

@RestController
@RequestMapping(value="/",produces = { "application/json", "text/json" })
public class TestController {
	@GetMapping
	public String helloworld(@RequestParam String number) throws Exception 
	{	
		return ReusableMethods.normalizePhoneNumber(number);
	}
}
