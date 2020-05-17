package com.ec.application.data;

import java.util.List;

import org.springframework.data.domain.Page;

public class DashBoardData 
{
	List<ReturnSingleNotification> notifications;
	List<DashboardInwardOutwardInventoryDAO> outwardInventory;
	List<DashboardInwardOutwardInventoryDAO> inwardInventory;
	List<DashboardMachineOnRentDAO> machineryOnRent;
	
	
	public List<DashboardMachineOnRentDAO> getMachineryOnRent() {
		return machineryOnRent;
	}
	public void setMachineryOnRent(List<DashboardMachineOnRentDAO> machineryOnRent) {
		this.machineryOnRent = machineryOnRent;
	}
	public List<ReturnSingleNotification> getNotifications() {
		return notifications;
	}
	public void setNotifications(List<ReturnSingleNotification> notifications) {
		this.notifications = notifications;
	}
	public List<DashboardInwardOutwardInventoryDAO> getOutwardInventory() {
		return outwardInventory;
	}
	public void setOutwardInventory(List<DashboardInwardOutwardInventoryDAO> outwardInventory) {
		this.outwardInventory = outwardInventory;
	}
	public List<DashboardInwardOutwardInventoryDAO> getInwardInventory() {
		return inwardInventory;
	}
	public void setInwardInventory(List<DashboardInwardOutwardInventoryDAO> inwardInventory) {
		this.inwardInventory = inwardInventory;
	}
	
	
	
	
}
