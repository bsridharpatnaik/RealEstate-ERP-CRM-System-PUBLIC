package com.ec.application.service;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.DashBoardData;

@Service
@Transactional
public class DashBoardService
{
	@Autowired
	AllNotificationService allNotificationService;

	@Autowired
	AllInventoryService allInventoryService;

	@Autowired
	StockService stockService;

	Logger log = LoggerFactory.getLogger(DashBoardService.class);

	public DashBoardData getContents()
	{
		DashBoardData dashBoardData = new DashBoardData();
		dashBoardData.setNotifications(allNotificationService.getAllNotifications().getNotifications());
		dashBoardData.setOutwardInventory(allInventoryService.fetchInventoryForDashboard("outward"));
		dashBoardData.setInwardInventory(allInventoryService.fetchInventoryForDashboard("inward"));
		dashBoardData.setMachineryOnRent(allInventoryService.fetchMachineryOnRent());
		dashBoardData.setStockPercent(stockService.fetchStockPercent());
		return dashBoardData;
	}
}
