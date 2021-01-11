package com.ec.crm.Data;

import java.io.Serializable;
import java.util.List;

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

	public UserReturnData(Long userId, String userName2, List<String> fetchRolesFromSet)
	{
		this.id = userId;
		this.username = userName2;
		this.roles = fetchRolesFromSet;
	}
}
