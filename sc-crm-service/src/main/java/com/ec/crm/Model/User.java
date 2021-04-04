package com.ec.crm.Model;

import org.hibernate.annotations.BatchSize;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "security_user", uniqueConstraints =
{ @UniqueConstraint(columnNames =
		{ "user_name" }) })
public class User
{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long userId;

	@NonNull
	@Column(unique = true, name = "user_name")
	private String userName;

	@NonNull
	private String password;

	@NonNull
	private boolean status;

	@NonNull
	private boolean passwordExpired;

	private String tenants;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "user_role", joinColumns =
	{ @JoinColumn(name = "userId", referencedColumnName = "userId") }, inverseJoinColumns =
	{ @JoinColumn(name = "role_name", referencedColumnName = "name") })

	@BatchSize(size = 20)
	private Set<Role> roles = new HashSet<>();

	public String getTenants()
	{
		return tenants;
	}

	public void setTenants(String tenants)
	{
		this.tenants = tenants;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public Long getUserId()
	{
		return userId;
	}

	public void setUserId(Long userId)
	{
		this.userId = userId;
	}

	public boolean isStatus()
	{
		return status;
	}

	public void setStatus(boolean status)
	{
		this.status = status;
	}

	public boolean isPasswordExpired()
	{
		return passwordExpired;
	}

	public void setPasswordExpired(boolean passwordExpired)
	{
		this.passwordExpired = passwordExpired;
	}

	public Set<Role> getRoles()
	{
		return roles;
	}

	public void setRoles(Set<Role> roles)
	{
		this.roles = roles;
	}
}
