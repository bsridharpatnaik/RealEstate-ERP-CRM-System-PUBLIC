package com.ec.application.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.ReturnAllNotificationsData;
import com.ec.application.data.ReturnSingleNotification;
import com.ec.application.model.InventoryNotification;

@Service
@Transactional
public class AllNotificationService
{
	@Autowired
	InventoryNotificationService inventoryNotificationService;
	Logger log = LoggerFactory.getLogger(AllNotificationService.class);

	public ReturnAllNotificationsData getAllNotifications()
//	{
		ReturnAllNotificationsData returnAllNotificationsData = new ReturnAllNotificationsData();
		List<ReturnSingleNotification> allNotifications = new ArrayList<ReturnSingleNotification>();
		allNotifications.addAll(getInventoryNotification());
		returnAllNotificationsData.setNotifications(allNotifications);
		return returnAllNotificationsData;
	}

	private List<ReturnSingleNotification> getInventoryNotification()
	{
		String source = "inventory";
		List<ReturnSingleNotification> inventoryNormalizedNotifications = new ArrayList<ReturnSingleNotification>();
		List<InventoryNotification> inventoryNotifications = inventoryNotificationService
				.returnInventoryNotifications();

		for (InventoryNotification inventoryNotification : inventoryNotifications)
		{
			DecimalFormat df = new DecimalFormat("###.#");
			String message = "";
			ReturnSingleNotification returnAllNotification = new ReturnSingleNotification();
			if (inventoryNotification.getType().equals("lowStock"))
			{
				message = "Product " + inventoryNotification.getProduct().getProductName()
						+ " is low on stock. Current Stock  - " + df.format(inventoryNotification.getQuantity());
				setFields(returnAllNotification, inventoryNotification.getId(), source, "lowStock", message,
						inventoryNotification.getCreationDate(), inventoryNotification.getLastModifiedDate(), false);
			}

			if (inventoryNotification.getType().equals("inwardStockModified"))
			{
				message = "Inward Quantity for Product -"
						+ inventoryNotification.getProduct().getProductName().toUpperCase() + " in Warehouse -"
						+ inventoryNotification.getWarehouseName().toUpperCase() + " is modified by User - "
						+ inventoryNotification.getUpdatedBy() + ". Updated Closing Stock  - "
						+ df.format(inventoryNotification.getQuantity());
				setFields(returnAllNotification, inventoryNotification.getId(), source, "inwardStockModified", message,
						inventoryNotification.getCreationDate(), inventoryNotification.getLastModifiedDate(), true);
			}

			if (inventoryNotification.getType().equals("outwardStockModified"))
			{
				message = "Outward Quantity for Product -"
						+ inventoryNotification.getProduct().getProductName().toUpperCase() + "in Warehouse -"
						+ inventoryNotification.getWarehouseName().toUpperCase() + " is modified by User - "
						+ inventoryNotification.getUpdatedBy() + ". Updated Closing Stock  - "
						+ df.format(inventoryNotification.getQuantity());
				setFields(returnAllNotification, inventoryNotification.getId(), source, "outwardStockModified", message,
						inventoryNotification.getCreationDate(), inventoryNotification.getLastModifiedDate(), true);
			}

			if (inventoryNotification.getType().equals("lostDamagedStockModified"))
			{
				message = "Quantity of Lost/Damaged Product -"
						+ inventoryNotification.getProduct().getProductName().toUpperCase() + "in Warehouse -"
						+ inventoryNotification.getWarehouseName().toUpperCase() + " is modified by User - "
						+ inventoryNotification.getUpdatedBy() + ". Updated Closing Stock  - "
						+ df.format(inventoryNotification.getQuantity());
				setFields(returnAllNotification, inventoryNotification.getId(), source, "lostDamagedStockModified",
						message, inventoryNotification.getCreationDate(), inventoryNotification.getLastModifiedDate(),
						true);
			}

			if (inventoryNotification.getType().equals("lostDamagedStockAdded"))
			{
				message = "New Lost/Damaged entry added for Product -"
						+ inventoryNotification.getProduct().getProductName().toUpperCase() + "in Warehouse -"
						+ inventoryNotification.getWarehouseName().toUpperCase() + " by User - "
						+ inventoryNotification.getUpdatedBy() + ". Updated Closing Stock  - "
						+ df.format(inventoryNotification.getQuantity());
				setFields(returnAllNotification, inventoryNotification.getId(), source, "lostDamagedStockAdded",
						message, inventoryNotification.getCreationDate(), inventoryNotification.getLastModifiedDate(),
						true);
			}
			inventoryNormalizedNotifications.add(returnAllNotification);
		}
		return inventoryNormalizedNotifications;
	}

	private void setFields(ReturnSingleNotification returnAllNotification, Long id, String source, String type,
			String message, Date created, Date modified, boolean b)
	{
		returnAllNotification.setCanBeDeleted(b);
		returnAllNotification.setCreated(created);
		returnAllNotification.setDeleted(false);
		returnAllNotification.setMessage(message);
		returnAllNotification.setModified(modified);
		returnAllNotification.setNotificationId(id);
		returnAllNotification.setSource(source);
		returnAllNotification.setType(type);
	}

	public void deleteNotification(Long id) throws Exception
	{
		inventoryNotificationService.deleteNotificationByID(id);
	}

}
