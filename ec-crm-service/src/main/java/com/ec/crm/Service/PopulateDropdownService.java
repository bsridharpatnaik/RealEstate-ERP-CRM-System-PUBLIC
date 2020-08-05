package com.ec.crm.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.NameAndProjectionDataForDropDown;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.SourceRepo;

@Service
public class PopulateDropdownService 
{

	/*
	 * @Autowired SentimentRepo sentimentRepo;
	 */
	
	@Autowired
	BrokerRepo brokerRepo;
	
	@Autowired
	SourceRepo sourceRepo;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	public NameAndProjectionDataForDropDown fetchData(String page) 
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		switch(page)
		{
			case "lead":
				morDropdownDataList.setBrokerDetails(brokerRepo.findIdAndNames());
				morDropdownDataList.setValidSentiments(SentimentEnum.getValidSentiments());
				morDropdownDataList.setSourceDetails(sourceRepo.findIdAndNames());
				morDropdownDataList.setValidPropertyType(PropertyTypeEnum.getValidPropertyType());
				morDropdownDataList.setValidStatusType(LeadStatusEnum.getValidLeadStatus());
				morDropdownDataList.setAssigneeDetails(new PopulateAssigneeList(userDetailsService.getUserList()));
				break;
		}
		return morDropdownDataList;
	}
}
