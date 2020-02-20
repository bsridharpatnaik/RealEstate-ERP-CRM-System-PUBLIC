package com.ec.application.data;

import java.io.Serializable;
import java.util.ArrayList;

public class JwtResponse implements Serializable{

	private static final long serialVersionUID = -8091879091924046844L;
	private String token;
	private ArrayList<String> roles;
	
	public JwtResponse(String token, ArrayList<String> roles) 
	{
		this.token=token;
		this.roles=roles;
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
