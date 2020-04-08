package com.ec.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
//import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@EnableZuulProxy
//@EnableEurekaServer
@SpringBootApplication
public class GatewayApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
