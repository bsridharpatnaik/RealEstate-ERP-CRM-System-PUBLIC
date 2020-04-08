package com.ec.application.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.application.data.UserReturnData;

@Service
public class UserDetailsService 
{
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Value("${gate.serverurl}")
	private String reqUrl;
    
	public UserReturnData getCurrentUser()
    {
    	System.out.println(request.getHeader("Authorization"));
    	UserReturnData userDetails = webClientBuilder.build()
					    	.get()
					    	.uri(reqUrl+"/user/me")
					    	.header("Authorization", request.getHeader("Authorization"))
					    	.retrieve()
					    	.bodyToMono(UserReturnData.class)
					    	.block();
    	return userDetails;
    }
}
