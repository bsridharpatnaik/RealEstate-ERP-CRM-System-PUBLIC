package com.ec.crm.Data;

public class AssigneeDAO 
{

	Long userid;
	String userName;
	public Long getUserid() {
		return userid;
	}
	public void setUserid(Long userid) {
		this.userid = userid;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public AssigneeDAO(Long userid, String userName) {
		super();
		this.userid = userid;
		this.userName = userName;
	}
	
	
}
