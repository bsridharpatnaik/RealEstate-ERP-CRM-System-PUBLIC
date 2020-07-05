package com.ec.crm.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Status;
import com.ec.crm.Repository.StatusRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StatusService {
	@Autowired
	StatusRepo sRepo;
	public Page<Status> fetchAll(Pageable pageable) 
	{
		return sRepo.findAll(pageable);
	}
	
	public Status createStatus(Status payload) throws Exception {
		if(!sRepo.existsByName(payload.getName()))
		{
			sRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Status already exists!");
		}
	}
	
	public Status findSingleStatus(long id) throws Exception 
	{
		Optional<Status> status = sRepo.findById(id);
		if(status.isPresent())
			return status.get();
		else
			throw new Exception("sentiment ID not found");
	}
	
	public Status updateStatus(Long id, Status status) throws Exception 
	{
		Optional<Status> StatusForUpdateOpt = sRepo.findById(id);
		Status StatusForUpdate = StatusForUpdateOpt.get();
		
		if(!StatusForUpdateOpt.isPresent())
			throw new Exception("Sentiment not found with sentimentid");
		
		if(!sRepo.existsByName(status.getName()) && !status.getName().equalsIgnoreCase(StatusForUpdate.getName()))
		{
			StatusForUpdate.setName(status.getName());
			
		}
        else 
        {
        	throw new Exception("Status with same Name already exists");
        }
		return sRepo.save(StatusForUpdate);
	}
	
	public void deleteStatus(Long id) throws Exception 
	{
		sRepo.softDeleteById(id);
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return sRepo.findIdAndNames();
	}
}
