package com.ec.application.data;

import java.util.List;

public class ReturnAllNotificationsData 
{
	List<ReturnSingleNotification> inventoryNormalizedNotifications;

	public List<ReturnSingleNotification> getInventoryNormalizedNotifications() {
		return inventoryNormalizedNotifications;
	}

	public void setInventoryNormalizedNotifications(List<ReturnSingleNotification> inventoryNormalizedNotifications) {
		this.inventoryNormalizedNotifications = inventoryNormalizedNotifications;
	}
	
}
