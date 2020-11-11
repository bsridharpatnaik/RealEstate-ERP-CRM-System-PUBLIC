package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Model.Role;
import com.ec.crm.Model.User;
import com.ec.crm.Repository.RoleRepo;
import com.ec.crm.Repository.UserRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService
{
	@Autowired
	UserRepo uRepo;

	@Autowired
	RoleRepo rRepo;

	// private static final Logger log = LoggerFactory.getLogger(UserService.class);

	public UserReturnData fetchUserDetailsById(Long id) throws Exception
	{
		UserReturnData userReturnData = new UserReturnData();
		Optional<User> userOpt = uRepo.findById(id);
		if (!userOpt.isPresent())
			throw new Exception("User not found with ID -" + id);

		User user = userOpt.get();
		userReturnData.setUsername(user.getUserName());
		userReturnData.setId(user.getUserId());
		userReturnData.setRoles(fetchRolesFromSet(user.getRoles()));
		return userReturnData;
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

	public List<UserReturnData> fetchUserList()
	{
		List<UserReturnData> userReturnDataList = new ArrayList<UserReturnData>();
		List<User> userList = uRepo.findAll();
		for (User user : userList)
		{
			UserReturnData userReturnData = new UserReturnData(user.getUserId(), user.getUserName(),
					fetchRolesFromSet(user.getRoles()));
			userReturnDataList.add(userReturnData);
		}
		return userReturnDataList;
	}

	public Map<Long, String> fetchUserListAsMap()
	{
		Map<Long, String> returndata = new HashMap<>();
		List<User> userList = uRepo.findAll();
		for (User user : userList)
		{
			returndata.put(user.getUserId(), user.getUserName());
		}
		return returndata;
	}

	public UserReturnData fetchUserDetailsByName(String name) throws Exception
	{
		UserReturnData userReturnData = new UserReturnData();
		ArrayList<User> userList = uRepo.findUserByUsername(name);
		if (userList.size() != 1)
			throw new Exception("User not found with username -" + name);

		User user = userList.get(0);
		userReturnData.setUsername(user.getUserName());
		userReturnData.setId(user.getUserId());
		userReturnData.setRoles(fetchRolesFromSet(user.getRoles()));
		return userReturnData;
	}

}
