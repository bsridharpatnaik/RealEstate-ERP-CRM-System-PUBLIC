package com.ec.application.data;

import java.util.ArrayList;

public class CreateUserData 
{
	String username;
	String password;
	ArrayList<String> roles;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public ArrayList<String> getRole() {
		return roles;
	}
	public void setRole(ArrayList<String> role) {
		this.roles = role;
	}
	
}
