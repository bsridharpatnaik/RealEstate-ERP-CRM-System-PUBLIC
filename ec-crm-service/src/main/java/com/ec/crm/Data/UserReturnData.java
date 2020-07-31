package com.ec.crm.Data;

import java.util.List;

import lombok.Data;
@Data
public class UserReturnData 
{
	String username;
	List<String> roles;
	Long id;	
}
