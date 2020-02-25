package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Machinery;
import com.ec.application.repository.MachineryRepo;


@Service
public class MachineryService 
{

	@Autowired
	MachineryRepo machineryRepo;
	
	public Page<Machinery> findAll(Pageable pageable)
	{
		return machineryRepo.findAll(pageable);
    }
	
	public Machinery createMachinery(Machinery payload) throws Exception 
	{
		if(!machineryRepo.existsByMachineryName(payload.getMachineryName()))
		{
			machineryRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Mahcinery already exists!");
		}
    }

	public Machinery updateMachinery(Long id, Machinery payload) throws Exception 
	{
		Optional<Machinery> MachineryForUpdateOpt = machineryRepo.findById(id);
        Machinery MachineryForUpdate = MachineryForUpdateOpt.get();
        
		Machinery newMachinery = new Machinery();
        newMachinery = payload;
        if(!machineryRepo.existsByMachineryName(newMachinery.getMachineryName()) && 
        		!newMachinery.getMachineryName().equalsIgnoreCase(MachineryForUpdate.getMachineryName()))
        {		
        	MachineryForUpdate.setMachineryName(newMachinery.getMachineryName());
            MachineryForUpdate.setMachineryDescription(newMachinery.getMachineryDescription());
        }
        else if(newMachinery.getMachineryName().equalsIgnoreCase(MachineryForUpdate.getMachineryName()))
        {
        	MachineryForUpdate.setMachineryDescription(newMachinery.getMachineryDescription());
        }
        
        else 
        {
        	throw new Exception("Machinery with same Name already exists");
        }
        	
        return machineryRepo.save(MachineryForUpdate);
        
    }

	public Machinery findSingleMachinery(Long id) 
	{
		Optional<Machinery> Machinerys = machineryRepo.findById(id);
		return Machinerys.get();
	}
	public void deleteMachinery(Long id) throws Exception 
	{
		try
		{
			machineryRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Machinery");
		}
	}

	public ArrayList<Machinery> findMachinerysByName(String name) 
	{
		ArrayList<Machinery> machinertList = new ArrayList<Machinery>();
		machinertList = machineryRepo.findBymachineryName(name);
		return machinertList;
	}

	public ArrayList<Machinery> findMachinerysByPartialName(String name) 
	{
		return machineryRepo.findByPartialName(name);
	}
}
