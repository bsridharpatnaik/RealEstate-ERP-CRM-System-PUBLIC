package com.ec.application.data;

import java.util.List;

public class DashBoardData 
{
	List<ReturnSingleNotification> notifications;
	List<DashboardInwardOutwardInventoryDAO> outwardInventory;
	List<DashboardInwardOutwardInventoryDAO> inwardInventory;
	
	
	public List<DashboardInwardOutwardInventoryDAO> getInwardInventory() {
		return inwardInventory;
	}

	public void setInwardInventory(List<DashboardInwardOutwardInventoryDAO> inwardInventory) {
		this.inwardInventory = inwardInventory;
	}

	public List<DashboardInwardOutwardInventoryDAO> getOutwardInventory() {
		return outwardInventory;
	}

	public void setOutwardInventory(List<DashboardInwardOutwardInventoryDAO> outwardInventory) {
		this.outwardInventory = outwardInventory;
	}

	public List<ReturnSingleNotification> getNotifications() {
		return notifications;
	}

	public void setNotifications(List<ReturnSingleNotification> notifications) {
		this.notifications = notifications;
	}
	
}
