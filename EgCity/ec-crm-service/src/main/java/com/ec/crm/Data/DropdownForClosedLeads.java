package com.ec.crm.Data;

import java.util.List;

import lombok.Data;

@Data
public class DropdownForClosedLeads
{
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
}
