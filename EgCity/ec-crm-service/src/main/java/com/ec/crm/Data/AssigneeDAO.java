package com.ec.crm.Data;

import lombok.Data;

@Data
public class AssigneeDAO 
{

	Long userid;
	String userName;
	
	public AssigneeDAO(Long userid, String userName) {
		super();
		this.userid = userid;
		this.userName = userName;
	}
	
	
}
