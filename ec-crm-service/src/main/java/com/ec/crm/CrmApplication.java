package com.ec.crm;

import java.util.Date;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import com.ec.crm.Service.LeadActivityService;
import com.ec.crm.Service.LeadService;

@SpringBootApplication
public class CrmApplication extends SpringBootServletInitializer
{
	@Autowired
	LeadActivityService leadActivityService;

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}
	
	@Bean
	public ModelMapper modelMapper() {
	    return new ModelMapper();
	}
}
