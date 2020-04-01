package com.ec.gateway.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.ec.gateway.bean.auth.Data.CreateUserData;
import com.ec.gateway.bean.auth.Data.ResetPasswordData;
import com.ec.gateway.bean.auth.Data.Role;
import com.ec.gateway.bean.auth.Data.UpdateRolesForUserData;
import com.ec.gateway.bean.auth.Data.User;
import com.ec.gateway.bean.auth.Data.UsersWithRoleNameListData;
import com.ec.gateway.repository.RoleRepo;
import com.ec.gateway.repository.UserRepo;

@Service
public class UserService 
{
	@Autowired
	UserRepo uRepo;

	@Autowired
	RoleRepo rRepo;
	
	public User createUser(CreateUserData userData) throws Exception
	{		
		String username = userData.getUsername();
		String password = userData.getPassword();
		ArrayList<String> roles = userData.getRole(); 
	
		if(uRepo.findUserByUsername(username).size()==0)
		{
			User user = new User();
			Set<Role> roleset = new HashSet<Role>();
			
			for(String role : roles)
			{
				Role roleEntity = rRepo.findByName(role);
				roleset.add(roleEntity);
			}
				user.setUserName(username);
				user.setStatus(true);
				user.setRoles(roleset);
				user.setPassword(bCryptPassword(password));
				user.setPasswordExpired(false);
				uRepo.save(user);
				return user;
	
		}
		else
		{
			throw new Exception("User already exists");
		}
	}

	public String bCryptPassword(String password)
	{
		String bcyptedPassword;
		bcyptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		return bcyptedPassword;
	}

	public UsersWithRoleNameListData fetchAll(Pageable pageable)
	{
		UsersWithRoleNameListData usersWithRoleNameListData = new UsersWithRoleNameListData();
		usersWithRoleNameListData.setUsers(uRepo.findAll(pageable));
		usersWithRoleNameListData.setRoles(rRepo.findRoleNames());
		return usersWithRoleNameListData;
	}
	
	public User resetPassword(ResetPasswordData rpData) throws Exception 
	{
		String username = rpData.getUsername();
		String password = rpData.getNewPassword();
		ArrayList<User> users = uRepo.findUserByUsername(username);
		if(users.size()==1)
		{
			User user = users.get(0);
			user.setPassword(bCryptPassword(password));
			uRepo.save(user);
			return user;
		}
		else 
		{
			throw new Exception ("No or Multiple users found by username!");
		}
	}
	public User updateRolesForUser(UpdateRolesForUserData upRoleData) throws Exception 
	{
		String username = upRoleData.getUsername();
		ArrayList<String> roles = upRoleData.getRoles();
		ArrayList<User> users = uRepo.findUserByUsername(username);
		if(users.size()==1)
		{
			User user = users.get(0);
			Set<Role> roleset = new HashSet<Role>();
			
			for(String role : roles)
			{
				Role roleEntity = rRepo.findByName(role);
				if(roleEntity!=null)
				roleset.add(roleEntity);
			}
			user.setRoles(roleset);
			uRepo.save(user);
			return user;
		}
		else 
		{
			throw new Exception ("No or Multiple users found by username!");
		}
	}
	}
