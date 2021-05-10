package com.ec.common.Data;

import java.util.List;

public class UserReturnData 
{
	String username;
	List<String> roles;
	Long id;
	public UserReturnData(Long userId, String userName2, List<String> fetchRolesFromSet) 
	{
		this.id=userId;
		this.username=userName2;
		this.roles=fetchRolesFromSet;
	}
	public UserReturnData() {
		// TODO Auto-generated constructor stub
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
}
