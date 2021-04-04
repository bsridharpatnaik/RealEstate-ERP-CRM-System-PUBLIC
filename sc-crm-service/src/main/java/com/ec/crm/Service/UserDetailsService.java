package com.ec.crm.Service;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.ec.crm.Model.UserDetails;
import com.ec.crm.Repository.UserDetailsRepo;
import com.ec.crm.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UserReturnData;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsService
{
	@Autowired
	WebClient.Builder webClientBuilder;

	@Autowired
	HttpServletRequest request;

	@Value("${common.serverurl}")
	private String reqUrl;

	@Value("${spring.profiles.active}")
	private String profile;

	@Autowired
	UserDetailsRepo udRepo;

	Logger log = LoggerFactory.getLogger(UserDetailsService.class);

	public List<UserReturnData> getUserList() throws Exception
	{
		log.info("Making API Call to fetch userid from name");
		try
		{
			log.info("Fetching userlist from database");
			String dbName = "";
			List<UserReturnData> userDetails = new ArrayList<UserReturnData>();
			if(profile.contains("ec-"))
				dbName = "egcity";
			else if (profile.contains("sc-"))
				dbName = "common";
			ThreadLocalStorage.setTenantName(dbName);
			List<UserDetails> userList =udRepo.findAll();
			for(UserDetails user:userList) {
				UserReturnData userReturnData = new UserReturnData(user.getUserId(), user.getUserName(),
							Arrays.asList(user.getRoles().split(",").clone()));
				if (userReturnData.getRoles().contains("CRM") || userReturnData.getRoles().contains("CRM-Manager"))
						userDetails.add(userReturnData);
			}
			ThreadLocalStorage.setTenantName(null);
			if (userDetails != null)
				return userDetails;
			else
				throw new Exception("Unable to fetch user details. Please log out and try again");
		} catch (Exception e)
		{
			log.info("Error API Call to fetch user from ID " + e);
			throw new Exception("Unable to fetch user details. Please log out and try again");
		}
	}

	public UserReturnData getCurrentUser() throws Exception
	{
		log.info("Making API Call to fetch current user");
		try
		{
				UserReturnData userDetails = webClientBuilder.build().get().uri(reqUrl + "user/me")
						.header("Authorization", request.getHeader("Authorization")).retrieve()
						.bodyToMono(UserReturnData.class).block();
			if (userDetails != null)
				return userDetails;
			else
				throw new Exception("Unable to fetch current user. Please log out and try again");
		} catch (Exception e)
		{
			log.info("Error API Call to fetch current user " + e);
			throw new Exception("Unable to fetch current user. Please log out and try again");
		}
	}

	public UserReturnData getUserFromId(Long assigneeId) throws Exception
	{
		log.info("Making API Call to fetch user from ID");
		try
		{

			UserReturnData userDetails = webClientBuilder.build().get().uri(reqUrl + "user/byid/" + assigneeId)
					.header("Authorization", request.getHeader("Authorization")).retrieve()
					.bodyToMono(UserReturnData.class).block();
			if (userDetails != null)
				return userDetails;
			else
				throw new Exception("Unable to fetch user details. Please log out and try again");
		} catch (Exception e)
		{
			log.info("Error API Call to fetch user from ID " + e);
			throw new Exception("Unable to fetch user details. Please log out and try again");
		}
	}

	public UserReturnData getIdFromUsername(String name) throws Exception
	{
		log.info("Making API Call to fetch userid from name");
		try
		{

			UserReturnData userDetails = webClientBuilder.build().get().uri(reqUrl + "user/byname/" + name)
					.header("Authorization", request.getHeader("Authorization")).retrieve()
					.bodyToMono(UserReturnData.class).block();
			if (userDetails != null)
				return userDetails;
			else
				throw new Exception("Unable to fetch user details. Please log out and try again");
		} catch (Exception e)
		{
			log.info("Error API Call to fetch user from ID " + e);
			throw new Exception("Unable to fetch user details. Please log out and try again");
		}
	}



	public Map<Long, String> fetchUserListAsMap() throws Exception {
		List<UserReturnData> userList = getUserList();
		Map<Long, String> userIdMap = new HashMap<>();
		for(UserReturnData ud :userList)
		{
			userIdMap.put(ud.getId(),ud.getUsername());
		}
		return userIdMap;
	}
}
