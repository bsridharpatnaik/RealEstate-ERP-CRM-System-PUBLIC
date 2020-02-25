package com.ec.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.ec.application.data.CreateUserData;
import com.ec.application.model.Role;
import com.ec.application.model.User;
import com.ec.application.repository.RoleRepo;
import com.ec.application.repository.UserRepo;

@Service
public class UserService 
{
	@Autowired
	UserRepo uRepo;

	@Autowired
	RoleRepo rRepo;
	
	public User createUser(CreateUserData userData)
	{		
		String username = userData.getUsername();
		String password = userData.getPassword();
		String role = userData.getRole(); 
	
		if(!rRepo.findByUserName(username))
		{
			User user = new User();
			
			Role roleEntity = rRepo.findByName(role);
			Set<Role> roleset = new HashSet<Role>();
			roleset.add(roleEntity);
			user.setUserName(username);
			user.setStatus(true);
			user.setRoles(roleset);
			user.setPassword(bCryptPassword(password));
			user.setPasswordExpired(false);
			uRepo.save(user);
			return user;
		}
	}

	public String bCryptPassword(String password)
	{
		String bcyptedPassword;
		bcyptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		return bcyptedPassword;
	}

	public Page<User> fetchAll(Pageable pageable)
	{
		return uRepo.findAll(pageable);
	}
	
	public void updateUser(String username, String password) throws Exception 
	{
		ArrayList<User> users = uRepo.findUserByUsername(username);
		if(users.size()==1)
		{
			User user = users.get(0);
			user.setPassword(bCryptPassword(password));
			uRepo.save(user);
		}
		else 
		{
			throw new Exception ("No or Multiple users found by username!");
		}
		}
	}
