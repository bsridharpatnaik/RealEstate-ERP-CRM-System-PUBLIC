package com.ec.application.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.application.data.UserReturnData;

@Service
public class UserDetailsService
{
	@Autowired
	WebClient.Builder webClientBuilder;

	/*
	 * @Autowired HttpServletRequest request;
	 */

	@Value("${common.serverurl}")
	private String reqUrl;

	public UserReturnData getCurrentUser() throws Exception
	{
		try
		{
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
					.getRequest();
			try
			{
				UserReturnData userDetails = webClientBuilder.build().get().uri(reqUrl + "user/me")
						.header("Authorization", request.getHeader("Authorization")).retrieve()
						.bodyToMono(UserReturnData.class).block();
				return userDetails;
			} catch (Exception e)
			{
				throw new Exception("Unable to fetch current user. Please contact system administrator.");
			}
		} catch (NullPointerException npe)
		{
			UserReturnData userDetails = new UserReturnData();
			userDetails.setId((long) 404);
			userDetails.setUsername("system");
			return userDetails;

		} catch (Exception e)
		{

			e.printStackTrace();
			throw new Exception("Unable to fetch current user. Please contact system administrator.");
		}
	}
}
