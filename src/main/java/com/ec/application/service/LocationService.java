package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.AllLocationWithNamesData;
import com.ec.application.model.Category;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.LocationRepo;
import com.ec.common.Filters.CategorySpecifications;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.LocationSpecifications;

@Service
public class LocationService 
{
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	
	
	public UsageLocation createLocation(UsageLocation payload) throws Exception 
	{
		if(!locationRepo.existsByLocationName(payload.getLocationName()))
		{
			locationRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Location already exists!");
		}
    }

	public UsageLocation updateLocation(Long id, UsageLocation payload) throws Exception 
	{
		Optional<UsageLocation> LocationForUpdateOpt = locationRepo.findById(id);
        UsageLocation LocationForUpdate = LocationForUpdateOpt.get();
        
		UsageLocation newLocation = new UsageLocation();
        newLocation = payload;
        if(!locationRepo.existsByLocationName(newLocation.getLocationName()) && 
        		!LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
        {		
        	LocationForUpdate.setLocationName(newLocation.getLocationName());
            LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
            LocationForUpdate.setUsageArea(newLocation.getUsageArea());
           
        }
        else if(LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
        {
        	LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
        	LocationForUpdate.setUsageArea(newLocation.getUsageArea());
        }
        else 
        {
        	throw new Exception("Location with same Name already exists");
        }
        	
        return locationRepo.save(LocationForUpdate);
        
    }

	public UsageLocation findSingleLocation(Long id) 
	{
		Optional<UsageLocation> Locations = locationRepo.findById(id);
		return Locations.get();
	}
	public void deleteLocation(Long id) throws Exception 
	{
		if(!checkBeforeDeleteService.isLocationUsed(id))
			locationRepo.softDeleteById(id);
		else
			throw new Exception("Location in use. Cannot delete.");
		
	}

	public List<IdNameProjections> findIdAndNames() 
	{
		return locationRepo.findIdAndNames();
	}

	public AllLocationWithNamesData findFilteredLocationsWithTA(FilterDataList filterDataList, Pageable pageable) 
	{
		AllLocationWithNamesData allLocationsWithNamesData = new AllLocationWithNamesData();
		Specification<UsageLocation> spec = LocationSpecifications.getSpecification(filterDataList);
		
		if(spec!=null) allLocationsWithNamesData.setLocations(locationRepo.findAll(spec, pageable));
		else allLocationsWithNamesData.setLocations(locationRepo.findAll(pageable));
		
		List<String> namesList = locationRepo.getNames();
		namesList.addAll(locationRepo.getUsageAreas());
		namesList = ReusableMethods.removeNullsFromStringList(namesList);
		allLocationsWithNamesData.setNames(namesList);
		return allLocationsWithNamesData;
	}

}
