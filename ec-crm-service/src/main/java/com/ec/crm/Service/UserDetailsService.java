package com.ec.crm.Service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	Logger log = LoggerFactory.getLogger(UserDetailsService.class);
    
	public UserReturnData getCurrentUser()
    {
		log.info("Making API Call to fetch current user");
		try
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
		catch(Exception e)
		{
			log.info("Error API Call to fetch current user " + e);
			return null;
		}
    }

	public UserReturnData getUserFromId(Long assigneeId) 
	{
		log.info("Making API Call to fetch user from ID");
		try
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
		catch(Exception e)
		{
			log.info("Error API Call to fetch user from ID " + e);
			return null;
		}
	}
	
	public UserReturnData[] getUserList() 
	{
		log.info("Making API Call to fetch userlist");
		try
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
		catch(Exception e)
		{
			log.info("Error API Call to fetch user list " + e);
			return null;
		}
		
	}
	
}
