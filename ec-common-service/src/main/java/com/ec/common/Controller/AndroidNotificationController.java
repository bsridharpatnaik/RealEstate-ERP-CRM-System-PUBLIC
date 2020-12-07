package com.ec.common.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Data.NotificationRequestDto;
import com.ec.common.Service.GatewayNotificationService;

@RestController
@RequestMapping("/notification")
public class AndroidNotificationController
{
	@Autowired
	GatewayNotificationService laNotificationService;

	@PostMapping("/send")
	public String sendPnsToDevice(@RequestBody NotificationRequestDto notificationRequestDto) throws Exception
	{
		return laNotificationService.sendPnsToDevice(notificationRequestDto);
	}
}
