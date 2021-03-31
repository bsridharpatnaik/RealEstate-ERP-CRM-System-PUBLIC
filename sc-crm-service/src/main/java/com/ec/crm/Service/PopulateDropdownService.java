package com.ec.crm.Service;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.NameAndProjectionDataForDropDown;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Enums.StagnantDropdownValues;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.PropertyNameRepo;
import com.ec.crm.Repository.PropertyTypeRepo;
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

	@Autowired
	PropertyNameRepo pnRepo;

	@Autowired
	PropertyTypeRepo ptRepo;

	public NameAndProjectionDataForDropDown fetchData(String page) throws Exception
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		switch (page)
		{
		case "lead":
			HashMap<String, String> actvityStatus = getValidActivityStatus();
			morDropdownDataList.setBrokerDetails(brokerRepo.findIdAndNames());
			morDropdownDataList.setValidSentiments(SentimentEnum.getValidSentiments());
			morDropdownDataList.setSourceDetails(sourceRepo.findIdAndNames());
			morDropdownDataList.setValidPropertyType(PropertyTypeEnum.getValidPropertyType());
			morDropdownDataList.setValidStatusType(LeadStatusEnum.getValidLeadStatus());
			morDropdownDataList.setAssigneeDetails(new PopulateAssigneeList(userDetailsService.getUserList()));
			morDropdownDataList.setActvityStatus(actvityStatus);
			morDropdownDataList.setValidActivityType(ActivityTypeEnum.getValidActivityTypes());
			morDropdownDataList.setKeyValueForStagnantDropdown(StagnantDropdownValues.getKeyValue());
			break;
		case "customer":
			morDropdownDataList.setBrokerDetails(brokerRepo.findIdAndNames());
			morDropdownDataList.setSourceDetails(sourceRepo.findIdAndNames());
			morDropdownDataList.setValidPropertyType(PropertyTypeEnum.getValidPropertyType());
			morDropdownDataList.setAssigneeDetails(new PopulateAssigneeList(userDetailsService.getUserList()));
			break;
		case "payment":
			morDropdownDataList.setAssigneeDetails(new PopulateAssigneeList(userDetailsService.getUserList()));
			morDropdownDataList.setValidPropertyNames(pnRepo.getUniqueNames());
			morDropdownDataList.setValidPropertyTypes(ptRepo.getUniqueNames());

			break;
		}
		return morDropdownDataList;
	}

	private HashMap<String, String> getValidActivityStatus()
	{
		HashMap<String, String> actvityStatus = new HashMap<String, String>();
		actvityStatus.put("Open", "true");
		actvityStatus.put("Closed", "false");
		return actvityStatus;
	}
}
