package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.BOQStatusLocationsForType;
import com.ec.application.data.BOQStatusTypeListWithConsumedUnitCount;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BuildingType;
import com.ec.application.repository.BOQStatusRepo;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.common.Filters.BOQStatusSpecification;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;

@Service
public class BOQStatusService
{

	@Autowired
	BOQStatusRepo boqStatusRepo;

	@Autowired
	BuildingTypeRepo btRepo;

	@Autowired
	LocationRepo locationRepo;

	Logger log = LoggerFactory.getLogger(BOQStatusService.class);

	public List<BOQStatus> fetchAllBOQRecord()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return boqStatusRepo.findAll();
	}

	public List<BOQStatusTypeListWithConsumedUnitCount> fetchBOQStatusTypeListWithConsumedUnitCount()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<BOQStatusTypeListWithConsumedUnitCount> list = new ArrayList<BOQStatusTypeListWithConsumedUnitCount>();
		List<BuildingType> btTypeList = btRepo.findAll();
		for (BuildingType bt : btTypeList)
		{
			BOQStatusTypeListWithConsumedUnitCount data = new BOQStatusTypeListWithConsumedUnitCount();
			Long boqCrossedCount = boqStatusRepo.getBOQCrossedCountForBT(bt.getTypeId());
			Long boqTotalCount = boqStatusRepo.getBOQTotalCountForBT(bt.getTypeId());
			data.setBoqCrossedUnitCount(boqCrossedCount);
			data.setTotalUnitCount(boqTotalCount);
			data.setTypeId(bt.getTypeId());
			data.setTypeName(bt.getTypeName());
			list.add(data);
		}
		return list;
	}

	public List<BOQStatusLocationsForType> getLocationWiseStatusForType(Long typeId, FilterDataList filterDataList)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		List<BOQStatusLocationsForType> data = boqStatusRepo.getLocationWiseStatusForType(typeId);
		for (FilterAttributeData attribute : filterDataList.getFilterData())
		{
			data = filterData(data, attribute);
		}
		return data;
	}

	public Page<BOQStatus> fetchPagedDataForLocation(Long id, Pageable pageable, FilterDataList filterDataList)
			throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<BOQStatus> spec = BOQStatusSpecification.getSpecification(id, filterDataList);
		Page<BOQStatus> data = spec == null ? boqStatusRepo.findAll(pageable) : boqStatusRepo.findAll(spec, pageable);
		return data;

	}

	private List<BOQStatusLocationsForType> filterData(List<BOQStatusLocationsForType> data,
			FilterAttributeData attribute)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (attribute.getAttrName().equalsIgnoreCase("locationname"))
		{
			data = data.stream().filter(
					c -> (attribute.getAttrValue().stream().map(String::toLowerCase).collect(Collectors.toList()))
							.contains(c.getLocationName().toLowerCase()))
					.collect(Collectors.toList());
		}

		if (attribute.getAttrName().equalsIgnoreCase("crossedBOQQuantity"))
		{
			data = data.stream().filter(
					c -> (attribute.getAttrValue().stream().map(String::toLowerCase).collect(Collectors.toList()))
							.contains(c.getCrossedBOQQuantity().toString().toLowerCase()))
					.collect(Collectors.toList());
		}
		return data;
	}

	public Page<BOQStatus> fetchAFilteredBOQRecord(Pageable pageable, FilterDataList filterDataList)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return boqStatusRepo.findAll(pageable);
	}

	public List<String> fetchInventoryNamesForLocation(Long id) throws Exception
	{
		/*
		 * if (!locationRepo.existsById(id)) throw new
		 * Exception("Bulding Unit Nout found by ID - " + id);
		 * 
		 * UsageLocation ul = locationRepo.findById(id).get(); if (ul.getBuildingType()
		 * != null) {
		 */
		return boqStatusRepo.fetchInventoryNamesForLocation(id);
		/*
		 * } else { Long typeId = ul.getBuildingType().getTypeId(); List<String>
		 * inventoryFromTypeIDOrLocationId =
		 * boqStatusRepo.fetchInventoryNamesForLocationOrType(id, typeId); return
		 * inventoryFromTypeIDOrLocationId; }
		 */
	}

}
