package com.ec.application.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.InventoryNotification;
import com.ec.application.model.Product;
import com.ec.application.repository.InventoryNotificationRepo;

@Service
public class InventoryNotificationService 
{

	@Autowired
	StockService stockService;
	
	@Autowired
	InventoryNotificationRepo inventoryNotificationRepo;
	
	final String lowStock = "lowStock";
	
	public void checkStockAndPushLowStockNotification(Product product)
	{
		Double currentStock = stockService.findTotalStockForProduct(product.getProductId());
		Double reorderQuantity = product.getReorderQuantity();
		currentStock = currentStock==null?0:currentStock;
		reorderQuantity = reorderQuantity==null?0:reorderQuantity;
		if(currentStock<=reorderQuantity && reorderQuantity>0)
			pushLowStockNotification(product);
		else if(currentStock>reorderQuantity && reorderQuantity>0)
			removeLowStockNotification(product);
	}

	private void removeLowStockNotification(Product product) 
	{
		List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo.findByProductAndType(product.getProductId(),lowStock);
		if(inventoryNotifications.size()>0)
		{
			for(InventoryNotification inventoryNotification : inventoryNotifications)
			{
				inventoryNotificationRepo.softDelete(inventoryNotification);
			}
			System.out.println("Deleted all low stock notification for product "+ product.getProductName());
		}
	}

	private void pushLowStockNotification(Product product) 
	{
		List<InventoryNotification> inventoryNotification = inventoryNotificationRepo.findByProductAndType(product.getProductId(),lowStock);
		if(inventoryNotification.size()==0)
		{
			InventoryNotification inventoryNotificationNew = new InventoryNotification();
			inventoryNotificationNew.setProduct(product);
			inventoryNotificationNew.setType(lowStock);
			inventoryNotificationRepo.save(inventoryNotificationNew);
		}
		else
		{
			System.out.println("Low stock notification already exists for produt "+product.getProductName());
		}
	}
	
	List<InventoryNotification> returnInventoryNotifications()
	{
		List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo.findAll();
		return inventoryNotifications;
	}
}
