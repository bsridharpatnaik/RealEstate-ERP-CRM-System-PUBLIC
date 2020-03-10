package com.ec.application.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Location;
import com.ec.application.repository.LocationRepo;

@Service
public class LocationService 
{
	@Autowired
	LocationRepo LocationRepo;
	
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
		try
		{
			LocationRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Location");
		}
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

	public ArrayList<?> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return LocationRepo.findIdAndNames();
	}
}
