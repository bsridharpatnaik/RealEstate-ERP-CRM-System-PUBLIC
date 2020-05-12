package com.ec.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.data.ReturnAllNotificationsData;
import com.ec.application.service.AllNotificationService;

@RestController
@RequestMapping("/notification")
public class NotificationController 
{
	@Autowired 
	AllNotificationService allNotificationService;
	
	@GetMapping
	public ReturnAllNotificationsData fetchAllNotifications()
	{
		return allNotificationService.getAllNotifications();
	}
}
