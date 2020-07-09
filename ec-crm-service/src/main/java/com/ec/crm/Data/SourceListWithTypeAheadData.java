package com.ec.crm.Data;

import java.util.List;

import org.springframework.data.domain.Page;
import com.ec.crm.Model.Source;

public class SourceListWithTypeAheadData 
{
	Page<Source> sourceDetails;

	public Page<Source> getSourceDetails() {
		return sourceDetails;
	}

	public void setSourceDetails(Page<Source> sourceDetails) {
		this.sourceDetails = sourceDetails;
	}

	
	
	
}
