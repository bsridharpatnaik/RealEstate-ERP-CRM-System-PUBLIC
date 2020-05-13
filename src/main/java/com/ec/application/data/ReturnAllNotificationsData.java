package com.ec.application.data;

import java.util.List;

public class ReturnAllNotificationsData 
{
	List<ReturnSingleNotification> notifications;

	public List<ReturnSingleNotification> getNotifications() {
		return notifications;
	}

	public void setNotifications(List<ReturnSingleNotification> notifications) {
		this.notifications = notifications;
	}
}
