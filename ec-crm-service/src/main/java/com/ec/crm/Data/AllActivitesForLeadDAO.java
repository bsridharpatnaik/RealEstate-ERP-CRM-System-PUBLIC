package com.ec.crm.Data;

import java.util.List;

import com.ec.crm.Model.LeadActivity;

import lombok.Data;

@Data
public class AllActivitesForLeadDAO 
{
	List<LeadActivity> pendingActivities;
	List<LeadActivity> pastActivities;
}
