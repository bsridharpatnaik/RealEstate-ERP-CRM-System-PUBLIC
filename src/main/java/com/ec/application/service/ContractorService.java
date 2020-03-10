package com.ec.application.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Contractor;
import com.ec.application.repository.ContractorRepo;


@Service
public class ContractorService 
{

	@Autowired
	ContractorRepo contractorRepo;
	
	public Page<Contractor> findAll(Pageable pageable)
	{
		return contractorRepo.findAll(pageable);
    }
	
	public Contractor createContractor(Contractor payload) throws Exception 
	{
		if(!contractorRepo.existsByContractorName(payload.getContractorName()))
		{
			contractorRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Contractor already exists!");
		}
    }

	public Contractor updateContractor(Long id, Contractor payload) throws Exception 
	{
		Optional<Contractor> ContractorForUpdateOpt = contractorRepo.findById(id);
        Contractor ContractorForUpdate = ContractorForUpdateOpt.get();
        
		Contractor newContractor = new Contractor();
        newContractor = payload;
        if(!contractorRepo.existsByContractorName(newContractor.getContractorName())
        		&& !newContractor.getContractorName().equals(ContractorForUpdate.getContractorName()))
        	
        {		
        	ContractorForUpdate.setContractorName(newContractor.getContractorName());
            ContractorForUpdate.setContractorDescription(newContractor.getContractorDescription());
        }
        else if(newContractor.getContractorName().equalsIgnoreCase(ContractorForUpdate.getContractorName()))
        {
        	ContractorForUpdate.setContractorDescription(newContractor.getContractorDescription());
        }
        else 
        {
        	throw new Exception("Contractor with same Name already exists");
        }
        	
        return contractorRepo.save(ContractorForUpdate);
        
    }

	public Contractor findSingleContractor(Long id) 
	{
		Optional<Contractor> Contractors = contractorRepo.findById(id);
		return Contractors.get();
	}
	public void deleteContractor(Long id) throws Exception 
	{
		try
		{
			contractorRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Contractor");
		}
	}

	public ArrayList<Contractor> findContractorsByName(String name) 
	{
		ArrayList<Contractor> contractorList = new ArrayList<Contractor>();
		contractorList = contractorRepo.findBycontractorName(name);
		return contractorList;
	}

	public ArrayList<Contractor> findContractorsByPartialName(String name) 
	{
		return contractorRepo.findByPartialName(name);
	}

	public ArrayList<?> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return contractorRepo.findIdAndNames();
	}
}
