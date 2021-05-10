package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Model.LeadActivity;

import lombok.Data;

@Data
public class AllActivitesForLeadDAO 
{
	private List<LeadActivityOnLeadInformationDTO> pendingActivities;
	private List<LeadActivityOnLeadInformationDTO> pastActivities;
}
