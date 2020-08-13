package com.ec.crm.Data;

import java.util.HashMap;
import java.util.List;

import com.ec.crm.ReusableClasses.IdNameProjections;
import com.ec.crm.Service.PopulateAssigneeList;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
@Data
public class NameAndProjectionDataForDropDown 
{
	List<String> validPropertyType;
	List<String> validStatusType;
	List<String> validSentiments;
	List<IdNameProjections> brokerDetails;
	List<IdNameProjections> sourceDetails;
	List<String> validActivityType;
	HashMap<String,String> actvityStatus;
	PopulateAssigneeList assigneeDetails;
}
