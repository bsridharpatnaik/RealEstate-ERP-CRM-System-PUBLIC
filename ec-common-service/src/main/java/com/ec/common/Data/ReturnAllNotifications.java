package com.ec.common.Data;

public class ReturnAllNotifications 
{
	Long notificationId;
	String type;
	String message;
	public ReturnAllNotifications(Long id, String type, String message2) 
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
