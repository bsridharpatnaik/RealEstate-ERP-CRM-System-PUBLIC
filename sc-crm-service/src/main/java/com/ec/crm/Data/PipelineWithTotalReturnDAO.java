package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Enums.LeadStatusEnum;
import lombok.Data;

@Data
public class PipelineWithTotalReturnDAO 
{
	int totalCount;
	private LeadStatusEnum leadStatus;
	List<PipelineSingleReturnDTO> leads;
}
