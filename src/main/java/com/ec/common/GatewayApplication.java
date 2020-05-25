package com.ec.common;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
//import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@EnableZuulProxy
//@EnableEurekaServer
@SpringBootApplication
public class GatewayApplication extends SpringBootServletInitializer{
	
	@PostConstruct
	public void started() {
	    //TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID_INDIA));
	    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}
	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
