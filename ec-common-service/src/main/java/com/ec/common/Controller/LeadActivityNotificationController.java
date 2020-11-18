package com.ec.common.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Service.LeadActivityNotificationService;

@RestController
@RequestMapping("/lanotification")
public class LeadActivityNotificationController
{
	@Autowired
	LeadActivityNotificationService laNotificationService;

	/*
	 * @PostMapping("/token") public String sendPnsToDevice(@RequestBody
	 * LANotificationRequestDto notificationRequestDto) { return
	 * laNotificationService.sendPnsToDevice(notificationRequestDto); }
	 */
}
