package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.BasicEntities.Location;
import com.ec.application.model.BasicEntities.UnloadingArea;
import com.ec.application.repository.LocationRepo;

@Service
public class LocationService 
{
	@Autowired
	LocationRepo LocationRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	public Page<Location> findAll(Pageable pageable)
	{
		return LocationRepo.findAll(pageable);
    }
	
	public Location createLocation(Location payload) throws Exception 
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

	public Location updateLocation(Long id, Location payload) throws Exception 
	{
		Optional<Location> LocationForUpdateOpt = LocationRepo.findById(id);
        Location LocationForUpdate = LocationForUpdateOpt.get();
        
		Location newLocation = new Location();
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

	public Location findSingleLocation(Long id) 
	{
		Optional<Location> Locations = LocationRepo.findById(id);
		return Locations.get();
	}
	public void deleteLocation(Long id) throws Exception 
	{
		if(!checkBeforeDeleteService.isLocationUsed(id))
			LocationRepo.softDeleteById(id);
		else
			throw new Exception("Location in use. Cannot delete.");
		
	}

	public ArrayList<Location> findLocationsByName(String name) 
	{
		ArrayList<Location> locationList = new ArrayList<Location>();
		locationList = LocationRepo.findByLocationName(name);
		return locationList;
	}

	public ArrayList<Location> findLocationsByPartialName(String name) 
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
		Optional<Location> location = LocationRepo.findById(id);
		if(location.isPresent())
			return true;
		else
			return false;
	}
}
