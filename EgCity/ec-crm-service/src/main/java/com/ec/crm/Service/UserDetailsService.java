package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.Role;
import com.ec.crm.Model.User;
import com.ec.crm.Repository.UserRepo;

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

	@Autowired
	UserRepo uRepo;

	Logger log = LoggerFactory.getLogger(UserDetailsService.class);

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

	public List<UserReturnData> getUserList() throws Exception
	{
		log.info("Fetching userlist from database");
		List<UserReturnData> userReturnDataList = new ArrayList<UserReturnData>();
		List<User> userList = uRepo.findAll(Sort.by(Sort.Direction.ASC, "userName"));
		for (User user : userList)
		{
			if (user.isStatus() == true)
			{
				UserReturnData userReturnData = new UserReturnData(user.getUserId(), user.getUserName(),
						fetchRolesFromSet(user.getRoles()));
				if (userReturnData.getRoles().contains("CRM") || userReturnData.getRoles().contains("CRM-Manager"))
					userReturnDataList.add(userReturnData);
			}
		}
		return userReturnDataList;

	}

	private List<String> fetchRolesFromSet(Set<Role> roleSet)
	{
		List<String> roles = new ArrayList<String>();
		for (Role role : roleSet)
		{
			roles.add(role.getName());
		}
		return roles;
	}

}
