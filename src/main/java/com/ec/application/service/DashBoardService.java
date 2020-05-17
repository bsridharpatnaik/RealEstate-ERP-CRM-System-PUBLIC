package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.DashBoardData;
import com.ec.application.repository.InwardInventoryRepo;

@Service
public class DashBoardService 
{
	@Autowired 
	AllNotificationService allNotificationService;
	
	@Autowired
	AllInventoryService allInventoryService;
	
	public DashBoardData getContents() 
	{
		DashBoardData dashBoardData = new DashBoardData();
		dashBoardData.setNotifications(allNotificationService.getAllNotifications().getNotifications());
		dashBoardData.setOutwardInventory(allInventoryService.fetchInventoryForDashboard("outward"));
		dashBoardData.setOutwardInventory(allInventoryService.fetchInventoryForDashboard("inward"));
		return dashBoardData;
	}
}
