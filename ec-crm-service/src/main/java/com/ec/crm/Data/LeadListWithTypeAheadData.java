package com.ec.crm.Data;


import org.springframework.data.domain.Page;

import com.ec.crm.Model.Lead;


public class LeadListWithTypeAheadData 
{
	Page<Lead> LeadDetails;

	public Page<Lead> getLeadDetails() {
		return LeadDetails;
	}

	public void setLeadDetails(Page<Lead> leadDetails) {
		this.LeadDetails = leadDetails;
	}

		
	
	
	
}
