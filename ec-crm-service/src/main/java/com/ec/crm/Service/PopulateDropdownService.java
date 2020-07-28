package com.ec.crm.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.NameAndProjectionDataForDropDown;
import com.ec.crm.Model.LeadStatusEnum;
import com.ec.crm.Model.PropertyTypeEnum;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.SentimentRepo;
import com.ec.crm.Repository.SourceRepo;

@Service
public class PopulateDropdownService 
{

	@Autowired
	SentimentRepo sentimentRepo;
	
	@Autowired
	BrokerRepo brokerRepo;
	
	@Autowired
	SourceRepo sourceRepo;
	
	public NameAndProjectionDataForDropDown fetchData(String page) 
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		switch(page)
		{
			case "lead":
				morDropdownDataList.setBrokerDetails(brokerRepo.findIdAndNames());
				morDropdownDataList.setSentimentDetails(sentimentRepo.findIdAndNames());
				morDropdownDataList.setSourceDetails(sourceRepo.findIdAndNames());
				morDropdownDataList.setValidPropertyType(PropertyTypeEnum.getValidPropertyType());
				morDropdownDataList.setValidStatusType(LeadStatusEnum.getValidLeadStatus());
				break;
		}
		return morDropdownDataList;
	}
}
