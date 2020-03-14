package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.BasicEntities.UnloadingArea;
import com.ec.application.model.BasicEntities.Vendor;
import com.ec.application.repository.UnloadingAreaRepo;

@Service
public class UnloadingAreaService 
{
	@Autowired
	UnloadingAreaRepo UnloadingAreaRepo;
	
	public Page<UnloadingArea> findAll(Pageable pageable)
	{
		return UnloadingAreaRepo.findAll(pageable);
    }
	
	public UnloadingArea createUnloadingArea(UnloadingArea payload) throws Exception 
	{
		if(!UnloadingAreaRepo.existsByUnloadingAreaName(payload.getUnloadingAreaName()))
		{
			UnloadingAreaRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("UnloadingArea already exists!");
		}
    }

	public UnloadingArea updateUnloadingArea(Long id, UnloadingArea payload) throws Exception 
	{
		Optional<UnloadingArea> UnloadingAreaForUpdateOpt = UnloadingAreaRepo.findById(id);
        UnloadingArea UnloadingAreaForUpdate = UnloadingAreaForUpdateOpt.get();
        
		UnloadingArea newUnloadingArea = new UnloadingArea();
        newUnloadingArea = payload;
        if(!UnloadingAreaRepo.existsByUnloadingAreaName(newUnloadingArea.getUnloadingAreaName()) && 
        		!UnloadingAreaForUpdate.getUnloadingAreaName().equalsIgnoreCase(newUnloadingArea.getUnloadingAreaName()))
        {		
        	UnloadingAreaForUpdate.setUnloadingAreaName(newUnloadingArea.getUnloadingAreaName());
            UnloadingAreaForUpdate.setUnloadingAreaDescription(newUnloadingArea.getUnloadingAreaDescription());
           
        }
        else if(UnloadingAreaForUpdate.getUnloadingAreaName().equalsIgnoreCase(newUnloadingArea.getUnloadingAreaName()))
        {
        	UnloadingAreaForUpdate.setUnloadingAreaDescription(newUnloadingArea.getUnloadingAreaDescription());
        }
        else 
        {
        	throw new Exception("UnloadingArea with same Name already exists");
        }
        	
        return UnloadingAreaRepo.save(UnloadingAreaForUpdate);
        
    }

	public UnloadingArea findSingleUnloadingArea(Long id) 
	{
		Optional<UnloadingArea> UnloadingAreas = UnloadingAreaRepo.findById(id);
		return UnloadingAreas.get();
	}
	public void deleteUnloadingArea(Long id) throws Exception 
	{
		try
		{
			UnloadingAreaRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete UnloadingArea");
		}
	}

	public ArrayList<UnloadingArea> findUnloadingAreasByName(String name) 
	{
		ArrayList<UnloadingArea> unloadingAreaList = new ArrayList<UnloadingArea>();
		unloadingAreaList = UnloadingAreaRepo.findByUnloadingAreaName(name);
		return unloadingAreaList;
	}

	public ArrayList<UnloadingArea> findUnloadingAreasByPartialName(String name) 
	{
		return UnloadingAreaRepo.findByPartialName(name);
	}

	public List<IdNameProjections> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return UnloadingAreaRepo.findIdAndNames();
	}
	public boolean checkIfUnloadingAreaExists(Long id)
	{
		Optional<UnloadingArea> unloadingArea = UnloadingAreaRepo.findById(id);
		if(unloadingArea.isPresent())
			return true;
		else
			return false;
	}
}
