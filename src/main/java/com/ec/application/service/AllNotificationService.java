package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.ReturnAllNotificationsData;
import com.ec.application.data.ReturnSingleNotification;
import com.ec.application.model.InventoryNotification;

@Service
public class AllNotificationService 
{
	@Autowired
	InventoryNotificationService inventoryNotificationService;

	public ReturnAllNotificationsData getAllNotifications() 
	{
		ReturnAllNotificationsData returnAllNotificationsData = new ReturnAllNotificationsData();
		List<ReturnSingleNotification> allNotifications = new ArrayList<ReturnSingleNotification>();
		allNotifications.addAll(getInventoryNotification());
		returnAllNotificationsData.setNotifications(allNotifications);
		return returnAllNotificationsData;
	}

	private List<ReturnSingleNotification> getInventoryNotification() 
	{
		String source = "inventory";
		List<ReturnSingleNotification> inventoryNormalizedNotifications = new ArrayList<ReturnSingleNotification> ();
		List<InventoryNotification> inventoryNotifications = inventoryNotificationService.returnInventoryNotifications();
		
		for(InventoryNotification inventoryNotification : inventoryNotifications)
		{
			
			String message ="";
			if(inventoryNotification.getType().equals("lowStock"))
			{
				message= "Product " + inventoryNotification.getProduct().getProductName() +" is low on stock";
			}
			ReturnSingleNotification returnAllNotification = new ReturnSingleNotification
					(inventoryNotification.getId(),source,"lowStock",message,inventoryNotification.getCreated(),inventoryNotification.getModified());
			inventoryNormalizedNotifications.add(returnAllNotification);
		}
		return inventoryNormalizedNotifications;
	}

}
