package com.ec.crm.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NoteRepo;

@Service
public class CheckBeforeDeleteService
{

	@Autowired
	LeadRepo lRepo;

	@Autowired
	LeadActivityRepo laRepo;

	@Autowired
	NoteRepo noteRepo;

	public boolean isBrokerUsed(Long brokerId)
	{
		if (lRepo.findBrokerUsageCount(brokerId) > 0)
			return true;
		else
			return false;
	}

	public boolean isSourceUsed(Long sourceId)
	{
		if (lRepo.findSourceUsageCount(sourceId) > 0)
			return true;
		else
			return false;
	}

	public boolean isLeadUsed(Long leadId)
	{
		if (laRepo.findLeadUsageCount(leadId) > 0 || noteRepo.findLeadUsageCount(leadId) > 0)
			return true;
		else
			return false;
	}

}
