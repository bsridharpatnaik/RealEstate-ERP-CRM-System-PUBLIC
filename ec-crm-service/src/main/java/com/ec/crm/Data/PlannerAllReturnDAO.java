package com.ec.crm.Data;

import java.util.HashMap;
import java.util.List;

import lombok.Data;

@Data
public class PlannerAllReturnDAO
{

	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
	HashMap<String, String> keyValueForStagnantDropdown;
	PlannerWithTotalReturnDAO call;
	PlannerWithTotalReturnDAO meeting;
	PlannerWithTotalReturnDAO property_visit;
	PlannerWithTotalReturnDAO deal_close;
	PlannerWithTotalReturnDAO reminder;
	PlannerWithTotalReturnDAO task;
	PlannerWithTotalReturnDAO message;
	PlannerWithTotalReturnDAO email;
	PlannerWithTotalReturnDAO deal_lost;
}
