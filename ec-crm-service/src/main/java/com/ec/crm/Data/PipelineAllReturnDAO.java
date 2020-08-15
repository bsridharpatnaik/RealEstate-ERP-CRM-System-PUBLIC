package com.ec.crm.Data;

import java.util.List;

import lombok.Data;

@Data
public class PipelineAllReturnDAO 
{
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
	PipelineWithTotalReturnDAO leadGeneration;
	PipelineWithTotalReturnDAO propertyVisit;
	PipelineWithTotalReturnDAO negotiation;
	PipelineWithTotalReturnDAO deal_close;
	int dealLostCount;
}
