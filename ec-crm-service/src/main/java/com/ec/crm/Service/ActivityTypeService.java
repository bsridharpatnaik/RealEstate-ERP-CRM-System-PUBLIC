package com.ec.crm.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.PropertyTypeListWithTypeAheadData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;

import com.ec.crm.Filters.SourceSpecifications;
import com.ec.crm.Model.ActivityType;

import com.ec.crm.Model.Source;
import com.ec.crm.Repository.ActivityTypeRepo;

import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ActivityTypeService {
	@Autowired
	ActivityTypeRepo aRepo;
	public Page<ActivityType> fetchAll(Pageable pageable) 
	{
		return aRepo.findAll(pageable);
	}


	public ActivityType createActivityType(ActivityType atype) throws Exception {
		if(!aRepo.existsByName(atype.getName()))
		{
			return aRepo.save(atype);
		}
		else
		{
			throw new Exception("ActivityType already exists!");
		}
	}
	
	public ActivityType findSingleActivityType(long id) throws Exception 
	{
		Optional<ActivityType> atype = aRepo.findById(id);
		if(atype.isPresent())
			return atype.get();
		else
			throw new Exception("ActivityType ID not found");
	}
	
	public ActivityType updateActivityType(Long id, ActivityType atype) throws Exception 
	{
		Optional<ActivityType> ActivityForUpdateOpt = aRepo.findById(id);
		ActivityType ActivityForUpdate = ActivityForUpdateOpt.get();
		
		if(!ActivityForUpdateOpt.isPresent())
			throw new Exception("ActivityType not found with activityTypeid");
	
		if(aRepo.existsByName(atype.getName()) && !atype.getName().equalsIgnoreCase(ActivityForUpdate.getName()))
		{
        	throw new Exception("ActivityType with same Name already exists");
        }
		ActivityForUpdate.setName(atype.getName());
		return aRepo.save(ActivityForUpdate);
	}
	
	public void deleteActivityType(Long id) throws Exception 
	{
		aRepo.softDeleteById(id);
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return aRepo.findIdAndNames();
	}
}
