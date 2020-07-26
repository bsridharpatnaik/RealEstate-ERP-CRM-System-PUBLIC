package com.ec.crm.Data;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.StatusEnum;

public class ReturnCreatedLead
{
	StatusEnum status;
	Lead lead;
	
	public ReturnCreatedLead(StatusEnum status, Lead lead) {
		super();
		this.status = status;
		this.lead = lead;
	}

	public StatusEnum getStatus() {
		return status;
	}

	public void setStatus(StatusEnum status) {
		this.status = status;
	}

	public Lead getLead() {
		return lead;
	}

	public void setLead(Lead lead) {
		this.lead = lead;
	}
	
}
