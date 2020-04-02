package com.ec.application;

import org.hibernate.annotations.Loader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableEurekaClient
//@EnableWebSecurity
public class EcApplication  extends SpringBootServletInitializer{
	private static final String ZONE_ID_INDIA = "Asia/Kolkata";
	
	@Bean
	public WebClient.Builder getWebClientBuilder()
	{
		return WebClient.builder();
	}		
	
	public static void main(String[] args) 
	{
		SpringApplication.run(EcApplication.class, args);
	}
	
}
