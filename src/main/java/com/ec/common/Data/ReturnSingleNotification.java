package com.ec.common.Data;

public class ReturnSingleNotification extends ReusableFields
{
	Long notificationId;
	String type;
	String message;
	
	
	public ReturnSingleNotification() {
		super();
	}
	public ReturnSingleNotification(Long id, String type, String message2) 
	{
		this.notificationId=id;
		this.type=type;
		this.message=message2;
	}
	public Long getNotificationId() {
		return notificationId;
	}
	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
