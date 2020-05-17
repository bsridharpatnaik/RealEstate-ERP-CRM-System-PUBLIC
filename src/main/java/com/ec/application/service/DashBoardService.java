package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.DashBoardData;

@Service
public class DashBoardService 
{
	@Autowired 
	AllNotificationService allNotificationService;
	
	public DashBoardData getContents() 
	{
		DashBoardData dashBoardData = new DashBoardData();
		dashBoardData.setNotifications(allNotificationService.getAllNotifications().getNotifications());
		return dashBoardData;
	}
}
