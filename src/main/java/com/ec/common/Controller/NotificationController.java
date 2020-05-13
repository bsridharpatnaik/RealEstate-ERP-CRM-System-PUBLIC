package com.ec.common.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.ReturnAllNotificationsData;
import com.ec.common.Service.AllNotificationService;

@RestController
@RequestMapping("/notification")
public class NotificationController 
{
	@Autowired 
	AllNotificationService allNotificationService;
	
	@GetMapping
	public ReturnAllNotificationsData fetchAllNotifications() throws Exception
	{
		return allNotificationService.getAllNotifications();
	}
}
