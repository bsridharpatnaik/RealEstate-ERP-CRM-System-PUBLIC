package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.BOQStatusLocationsForType;
import com.ec.application.data.BOQStatusTypeListWithConsumedUnitCount;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BuildingType;
import com.ec.application.repository.BOQStatusRepo;
import com.ec.application.repository.BuildingTypeRepo;
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

	public List<BOQStatusLocationsForType> getLocationWiseStatusForType(Long typeId)
	{
		List<BOQStatusLocationsForType> data = boqStatusRepo.getLocationWiseStatusForType(typeId);
		return data;
	}

	public Page<BOQStatus> fetchAFilteredBOQRecord(Pageable pageable, FilterDataList filterDataList)
	{
		return boqStatusRepo.findAll();
	}

}
