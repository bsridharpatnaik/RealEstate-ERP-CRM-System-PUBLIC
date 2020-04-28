package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.repository.InwardInventoryRepo;
import com.ec.application.repository.LostDamagedInventoryRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.application.repository.OutwardInventoryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockRepo;
import com.ec.application.repository.WarehouseRepo;

@Service
public class CheckBeforeDeleteService 
{
	
	@Autowired
	InwardInventoryRepo inwardInventoryRepo;
	
	@Autowired
	MachineryOnRentRepo machineryOnRentRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	StockRepo stockRepo;
	
	//@Autowired
	//WarehouseRepo warehouseRepo;
	
	@Autowired
	LostDamagedInventoryRepo lostDamagedInventoryRepo;
	
	public boolean isProductUsed(Long productId) throws Exception
	{
		if(stockRepo.productUsageCount(productId) >0
				|| lostDamagedInventoryRepo.productUsageCount(productId) > 0)
			return true;
		else
			return false;
		
	}
	
	public boolean isCategoryUsed(Long categoryId) throws Exception
	{
		if(productRepo.categoryUsageCount(categoryId) > 0)
			return true;
		else
			return false;
		
	}
	
	public boolean isMachineryUsed(Long machineryId) throws Exception
	{
		if(machineryOnRentRepo.machineryUsageCount(machineryId) > 0)
			return true;
		else
			return false;
	}
	

	public boolean isLocationUsed(Long locationId) throws Exception
	{
		if(machineryOnRentRepo.locationUsageCount(locationId) > 0 
				//|| outwardInventoryRepo.locationUsageCount(locationId) > 0
				) 
			return true;
		else
			return false;
	}
	public boolean isunloadingAreaUsed(Long unloadingAreaId) throws Exception
	{
		/*if(inwardInventoryRepo.unloadingAreaCount(unloadingAreaId) > 0) 
			return true;
		else*/
			return false;
	}
	public boolean isWarehouseUsed(String warehouseName) throws Exception
	{
		if(stockRepo.warehouseCount(warehouseName) > 0) 
			return true;
		else
			return false;
	}
}

