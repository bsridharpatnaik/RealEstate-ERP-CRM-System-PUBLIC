package com.ec.application.data;

import org.springframework.lang.NonNull;

public class DeleteNotificationData 
{
	@NonNull
	Long notificationId;
	@NonNull
	String type;
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
}
