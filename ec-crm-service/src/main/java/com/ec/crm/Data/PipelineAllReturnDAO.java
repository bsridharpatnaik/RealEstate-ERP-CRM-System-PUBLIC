package com.ec.crm.Data;

import java.util.HashMap;
import java.util.List;

import lombok.Data;

@Data
public class PipelineAllReturnDAO
{
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
	HashMap<String, String> keyValueForStagnantDropdown;
	PipelineWithTotalReturnDAO leadGeneration;
	PipelineWithTotalReturnDAO propertyVisit;
	PipelineWithTotalReturnDAO negotiation;
	PipelineWithTotalReturnDAO deal_close;
}
