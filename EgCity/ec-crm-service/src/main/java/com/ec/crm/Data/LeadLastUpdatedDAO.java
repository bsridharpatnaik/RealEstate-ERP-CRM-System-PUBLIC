package com.ec.crm.Data;

import java.util.Date;

import lombok.Data;

@Data
public class LeadLastUpdatedDAO 
{
	Long id;
	Date lastUpdatedDate;
	public LeadLastUpdatedDAO(Long id, Date lastUpdatedDate) {
		super();
		this.id = id;
		this.lastUpdatedDate = lastUpdatedDate;
	}
}
