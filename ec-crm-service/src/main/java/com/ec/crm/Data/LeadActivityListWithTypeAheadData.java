package com.ec.crm.Data;

import java.util.HashMap;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Data;

@Data
public class LeadActivityListWithTypeAheadData
{
	Page<LeadPageData> LeadPageDetails;
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
	HashMap<String, String> keyValueForStagnantDropdown;
}
