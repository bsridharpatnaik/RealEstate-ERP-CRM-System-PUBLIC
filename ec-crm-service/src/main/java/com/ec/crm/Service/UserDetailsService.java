package com.ec.crm.Service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UserReturnData;


@Service
public class UserDetailsService 
{
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Value("${common.serverurl}")
	private String reqUrl;
    
	public UserReturnData getCurrentUser()
    {
    	UserReturnData userDetails = webClientBuilder.build()
					    	.get()
					    	.uri(reqUrl+"user/me")
					    	.header("Authorization", request.getHeader("Authorization"))
					    	.retrieve()
					    	.bodyToMono(UserReturnData.class)
					    	.block();
    	return userDetails;
    }

	public UserReturnData getUserFromId(Long assigneeId) 
	{
		UserReturnData userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/byid/"+assigneeId)
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData.class)
		    	.block();
		return userDetails;
	}
	
	public UserReturnData[] getUserList() 
	{
		UserReturnData[] userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/list")
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData[].class)
		    	.block();
		return userDetails;
	}
	
}
