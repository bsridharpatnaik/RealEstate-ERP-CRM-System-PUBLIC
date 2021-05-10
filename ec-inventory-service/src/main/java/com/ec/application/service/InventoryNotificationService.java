package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ec.application.model.InventoryNotification;
import com.ec.application.model.Product;
import com.ec.application.repository.InventoryNotificationRepo;
import com.ec.application.repository.WarehouseRepo;

@Service
public class InventoryNotificationService
{

	@Autowired
	StockService stockService;

	@Autowired
	InventoryNotificationRepo inventoryNotificationRepo;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	WarehouseRepo warehouseRepo;

	final String lowStock = "lowStock";
	final String inwardModified = "inwardStockModified";
	final String outwardModified = "outwardStockModified";
	final String lostDamagedModified = "lostDamagedStockModified";
	final String lostDamagedAdded = "lostDamagedStockAdded";

	@Transactional(rollbackOn = Exception.class)
	public void checkStockAndPushLowStockNotification(Product product)
	{
		Double currentStock = stockService.findTotalStockForProduct(product.getProductId());
		Double reorderQuantity = product.getReorderQuantity();
		currentStock = currentStock == null ? 0 : currentStock;
		reorderQuantity = reorderQuantity == null ? 0 : reorderQuantity;
		if (currentStock <= reorderQuantity && reorderQuantity > 0)
			pushLowStockNotification(product, currentStock);
		else if (currentStock > reorderQuantity && reorderQuantity > 0)
			removeLowStockNotification(product);
	}

	@Transactional(rollbackOn = Exception.class)
	public void pushQuantityEditedNotification(Product product, String warehouseName, String type, Double currentStock)
			throws Exception
	{

		type = getTypeFromReadableName(type);
		InventoryNotification inventoryNotificationNew = new InventoryNotification();
		inventoryNotificationNew.setProduct(product);
		inventoryNotificationNew.setType(type);
		inventoryNotificationNew.setQuantity(currentStock);
		inventoryNotificationNew.setUpdatedBy(userDetailsService.getCurrentUser().getUsername());
		inventoryNotificationNew.setWarehouseName(warehouseName);
		inventoryNotificationRepo.save(inventoryNotificationNew);
	}

	private String getTypeFromReadableName(String type)
	{
		switch (type)
		{
		case "inward":
			return inwardModified;

		case "outward":
			return outwardModified;

		case "lostdamagedmodified":
			return lostDamagedModified;

		case "lostdamagedadded":
			return lostDamagedAdded;
		}
		return "";
	}

	@Transactional(rollbackOn = Exception.class)
	private void removeLowStockNotification(Product product)
	{
		List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
				.findByProductAndType(product.getProductId(), lowStock);
		if (inventoryNotifications.size() > 0)
		{
			for (InventoryNotification inventoryNotification : inventoryNotifications)
			{
				inventoryNotificationRepo.softDelete(inventoryNotification);
			}
			System.out.println("Deleted all low stock notification for product " + product.getProductName());
		}
	}

	@Transactional(rollbackOn = Exception.class)
	private void pushLowStockNotification(Product product, Double currentStock)
	{
		List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
				.findByProductAndType(product.getProductId(), lowStock);
		if (inventoryNotifications.size() == 0)
		{
			InventoryNotification inventoryNotificationNew = new InventoryNotification();
			inventoryNotificationNew.setProduct(product);
			inventoryNotificationNew.setType(lowStock);
			inventoryNotificationNew.setQuantity(currentStock);
			inventoryNotificationNew.setUpdatedBy("System");
			inventoryNotificationRepo.save(inventoryNotificationNew);
		} else
		{
			System.out.println("Low stock notification already exists for produt " + product.getProductName());
			InventoryNotification inventoryNotification = inventoryNotifications.get(0);
			inventoryNotification.setQuantity(currentStock);
			inventoryNotificationRepo.save(inventoryNotification);
		}
	}

	List<InventoryNotification> returnInventoryNotifications()
	{
		List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
				.findAll(Sort.by(Sort.Direction.DESC, "modified"));
		return inventoryNotifications;
	}

	@Transactional(rollbackOn = Exception.class)
	public void deleteNotificationByID(Long id) throws Exception
	{
		Optional<InventoryNotification> inventoryNotificationOpt = inventoryNotificationRepo.findById(id);

		if (!inventoryNotificationOpt.isPresent())
			throw new Exception("Notification not found");

		InventoryNotification inventoryNotification = inventoryNotificationOpt.get();
		if (inventoryNotification.getType().equals("lowStock"))
			throw new Exception("Cannot delete lowStock notifications");

		inventoryNotificationRepo.softDelete(inventoryNotification);
	}
}
