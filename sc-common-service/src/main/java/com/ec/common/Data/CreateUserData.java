package com.ec.common.Data;

import java.util.ArrayList;

public class CreateUserData
{
	String username;
	String password;
	ArrayList<String> roles;
	ArrayList<String> tenants;

	public ArrayList<String> getTenants()
	{
		return tenants;
	}

	public void setTenants(ArrayList<String> tenants)
	{
		this.tenants = tenants;
	}

	public ArrayList<String> getRoles()
	{
		return roles;
	}

	public void setRoles(ArrayList<String> roles)
	{
		this.roles = roles;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public ArrayList<String> getRole()
	{
		return roles;
	}

	public void setRole(ArrayList<String> role)
	{
		this.roles = role;
	}

}
