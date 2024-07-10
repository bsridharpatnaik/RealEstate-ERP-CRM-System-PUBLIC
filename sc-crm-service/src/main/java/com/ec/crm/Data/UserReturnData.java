package com.ec.crm.Data;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserReturnData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String username;
	List<String> roles;
	Long id;
	String email;

	public UserReturnData(Long userId, String userName2, List<String> fetchRolesFromSet, String email)
	{
		this.id = userId;
		this.username = userName2;
		this.roles = fetchRolesFromSet.stream().map(String::toLowerCase).collect(Collectors.toList());
		this.email = email;
	}
}
