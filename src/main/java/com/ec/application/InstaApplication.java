package com.ec.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@SpringBootApplication
public class InstaApplication  extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(InstaApplication.class, args);
	}
	
}
