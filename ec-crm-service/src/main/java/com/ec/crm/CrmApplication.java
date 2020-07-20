package com.ec.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class CrmApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}

}
