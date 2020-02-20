package com.ec.application.data;

import java.io.Serializable;

public class JwtResponse implements Serializable{

	private static final long serialVersionUID = -8091879091924046844L;
	private String token;
	private String role;
	
	public JwtResponse(String token, String role)
	{
		this.token=token;
		this.role=role;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public JwtResponse(String token2) {
		this.token=token2;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
}
