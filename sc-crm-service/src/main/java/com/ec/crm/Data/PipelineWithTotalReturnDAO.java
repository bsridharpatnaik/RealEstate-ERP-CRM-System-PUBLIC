package com.ec.crm.Data;

import java.util.List;

import lombok.Data;

@Data
public class PipelineWithTotalReturnDAO 
{
	int totalCount;
	List<PipelineSingleReturnDTO> leads;
}
