package com.ec.application.data;

import java.io.Serializable;
import java.util.ArrayList;

public class JwtResponse implements Serializable{

	private static final long serialVersionUID = -8091879091924046844L;
	private String token;
	private String name;
	private ArrayList<String> roles;
	
	public JwtResponse(String name, String token, ArrayList<String> roles) 
	{
		this.name=name;
		this.token=token;
		this.roles=roles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public ArrayList<String> getRoles() {
		return roles;
	}
	public void setRoles(ArrayList<String> roles) {
		this.roles = roles;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
	
}
