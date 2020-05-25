package com.ec.common.Data;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.common.Model.User;

public class UserListWithTypeAheadData 
{
	List<String> roles;
	List<String> usernames;
	Page<User> userDetails;
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	public List<String> getUsernames() {
		return usernames;
	}
	public void setUsernames(List<String> usernames) {
		this.usernames = usernames;
	}
	public Page<User> getUserDetails() {
		return userDetails;
	}
	public void setUserDetails(Page<User> userDetails) {
		this.userDetails = userDetails;
	}
	
	
}
