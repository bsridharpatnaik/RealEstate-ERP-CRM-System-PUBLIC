package com.ec.application.service;

import java.awt.Checkbox;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Contractor;
import com.ec.application.repository.ContractorRepo;

@Service
public class ContractorService 
{
	@Autowired
	ContractorRepo contractorRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	public List<IdNameProjections> getContractorNames()
	{
		List<IdNameProjections> contractorNames = new ArrayList<IdNameProjections>();
		contractorNames = contractorRepo.findIdAndNames();
		return contractorNames;
	}

	public Page<Contractor> findAll(Pageable pageable) 
	{
		return contractorRepo.findAll(pageable);
	}
	
	public boolean isContactUsed(Long id) 
	{
		boolean isContactUsed = false;
		if(checkBeforeDeleteService.isContractorUsed(id))
			isContactUsed = true;
		return isContactUsed;
	}
}
