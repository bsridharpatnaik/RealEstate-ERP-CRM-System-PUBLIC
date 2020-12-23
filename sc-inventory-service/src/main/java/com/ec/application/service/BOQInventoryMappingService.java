package com.ec.application.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.BOQRequestData;
import com.ec.application.model.BOQInventoryMapping;
import com.ec.application.model.BOQLocationTypeEnum;
import com.ec.application.model.BuildingType;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.BOQInventoryMappingRepo;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.ProductRepo;

@Service
@Transactional
public class BOQInventoryMappingService
{

	@Autowired
	BOQInventoryMappingRepo bimRepo;

	@Autowired
	BuildingTypeRepo btRepo;

	@Autowired
	LocationRepo lRepo;

	@Autowired
	ProductRepo pRepo;

	public void createNewBOQ(BOQRequestData payload) throws Exception
	{
		validatePayload(payload);
		exitIfCombinationExists(payload);
		BOQInventoryMapping bim = new BOQInventoryMapping();
		setFields(bim, payload);
		bimRepo.save(bim);
	}

	private void setFields(BOQInventoryMapping bim, BOQRequestData payload)
	{
		if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType))
		{
			bim.setBuildingType(btRepo.findById(payload.getId()).get());
			bim.setBoqType(BOQLocationTypeEnum.BuildingType);

		} else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit))
		{
			bim.setLocation(lRepo.findById(payload.getId()).get());
			bim.setBoqType(BOQLocationTypeEnum.BuildingUnit);
		}
		bim.setProduct(pRepo.findById(payload.getProductId()).get());
		bim.setQuantity(payload.getQuantity());
	}

	private void exitIfCombinationExists(BOQRequestData payload) throws Exception
	{
		if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType))
		{
			BuildingType bt = btRepo.findById(payload.getId()).get();
			List<BOQInventoryMapping> bimList = bimRepo.findByBuildingTypeAndProduct(bt, payload.getProductId());
			if (bimList.size() > 0)
				throw new Exception("Inventory already exists for Building Type -" + bt.getTypeName());
		} else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit))
		{
			UsageLocation ul = lRepo.findById(payload.getId()).get();
			List<BOQInventoryMapping> bimList = bimRepo.findByLocationProduct(ul, payload.getProductId());
			if (bimList.size() > 0)
				throw new Exception("Inventory already exists for Building Unit -" + ul.getLocationName());
		}
	}

	private void validatePayload(BOQRequestData payload) throws Exception
	{
		if (payload.getBoqType() == null)
			throw new Exception("BOQ Type cannot be null or empty");

		if (payload.getId() == null)
			throw new Exception("Required field ID cannot be null or empty");
		if (payload.getProductId() == null)
			throw new Exception("Required field ProductID cannot be null or empty");
		if (payload.getQuantity() == null || payload.getQuantity() <= 0)
			throw new Exception("Required field Quantity cannot be empty OR less than 1");

		if (!pRepo.existsById(payload.getProductId()))
			throw new Exception("Product not found with ID -" + payload.getProductId());

		if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingType))
		{
			if (!btRepo.existsById(payload.getId()))
				throw new Exception("Building Type not found with ID -" + payload.getId());
		} else if (payload.getBoqType().equals(BOQLocationTypeEnum.BuildingUnit))
		{
			if (!lRepo.existsById(payload.getId()))
				throw new Exception("Building Unit not found with ID -" + payload.getId());
		}
	}
}
