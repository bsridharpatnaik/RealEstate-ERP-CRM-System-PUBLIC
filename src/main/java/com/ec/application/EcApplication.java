package com.ec.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class EcApplication  extends SpringBootServletInitializer{
	private static final String ZONE_ID_INDIA = "Asia/Kolkata";
			
	public static void main(String[] args) 
	{
		SpringApplication.run(EcApplication.class, args);
	}
	
}
