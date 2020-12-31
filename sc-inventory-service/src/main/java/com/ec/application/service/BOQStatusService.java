package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.BOQStatusLocationsForType;
import com.ec.application.data.BOQStatusTypeListWithConsumedUnitCount;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BuildingType;
import com.ec.application.repository.BOQStatusRepo;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.common.Filters.FilterAttributeData;
import com.ec.common.Filters.FilterDataList;

@Service
public class BOQStatusService
{

	@Autowired
	BOQStatusRepo boqStatusRepo;

	@Autowired
	BuildingTypeRepo btRepo;

	public List<BOQStatus> fetchAllBOQRecord()
	{
		return boqStatusRepo.findAll();
	}

	public List<BOQStatusTypeListWithConsumedUnitCount> fetchBOQStatusTypeListWithConsumedUnitCount()
	{
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
		List<BOQStatusLocationsForType> data = boqStatusRepo.getLocationWiseStatusForType(typeId);
		for (FilterAttributeData attribute : filterDataList.getFilterData())
		{
			data = filterData(data, attribute);
		}
		return data;
	}

	private List<BOQStatusLocationsForType> filterData(List<BOQStatusLocationsForType> data,
			FilterAttributeData attribute)
	{
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

	public List<BOQStatus> fetchAFilteredBOQRecord(Pageable pageable, FilterDataList filterDataList)
	{
		return boqStatusRepo.findAll();
	}

}
