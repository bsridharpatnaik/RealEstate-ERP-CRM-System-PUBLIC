package com.ec.common.Data;

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
