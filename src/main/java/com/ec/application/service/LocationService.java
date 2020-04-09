package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.LocationRepo;

@Service
public class LocationService 
{
	@Autowired
	LocationRepo LocationRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	public Page<UsageLocation> findAll(Pageable pageable)
	{
		return LocationRepo.findAll(pageable);
    }
	
	public UsageLocation createLocation(UsageLocation payload) throws Exception 
	{
		if(!LocationRepo.existsByLocationName(payload.getLocationName()))
		{
			LocationRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Location already exists!");
		}
    }

	public UsageLocation updateLocation(Long id, UsageLocation payload) throws Exception 
	{
		Optional<UsageLocation> LocationForUpdateOpt = LocationRepo.findById(id);
        UsageLocation LocationForUpdate = LocationForUpdateOpt.get();
        
		UsageLocation newLocation = new UsageLocation();
        newLocation = payload;
        if(!LocationRepo.existsByLocationName(newLocation.getLocationName()) && 
        		!LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
        {		
        	LocationForUpdate.setLocationName(newLocation.getLocationName());
            LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
           
        }
        else if(LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
        {
        	LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
        }
        else 
        {
        	throw new Exception("Location with same Name already exists");
        }
        	
        return LocationRepo.save(LocationForUpdate);
        
    }

	public UsageLocation findSingleLocation(Long id) 
	{
		Optional<UsageLocation> Locations = LocationRepo.findById(id);
		return Locations.get();
	}
	public void deleteLocation(Long id) throws Exception 
	{
		if(!checkBeforeDeleteService.isLocationUsed(id))
			LocationRepo.softDeleteById(id);
		else
			throw new Exception("Location in use. Cannot delete.");
		
	}

	public ArrayList<UsageLocation> findLocationsByName(String name) 
	{
		ArrayList<UsageLocation> locationList = new ArrayList<UsageLocation>();
		locationList = LocationRepo.findByLocationName(name);
		return locationList;
	}

	public ArrayList<UsageLocation> findLocationsByPartialName(String name) 
	{
		return LocationRepo.findByPartialName(name);
	}

	public List<IdNameProjections> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return LocationRepo.findIdAndNames();
	}
	
	public boolean checkIfUnloadingAreaExists(Long id)
	{
		Optional<UsageLocation> location = LocationRepo.findById(id);
		if(location.isPresent())
			return true;
		else
			return false;
	}
}
