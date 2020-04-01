package com.ec.gateway.bean.auth.Data;

import java.util.ArrayList;

public class UpdateRolesForUserData 
{
	String username;
	ArrayList<String> roles;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public ArrayList<String> getRoles() {
		return roles;
	}
	public void setRoles(ArrayList<String> roles) {
		this.roles = roles;
	}
	

}
