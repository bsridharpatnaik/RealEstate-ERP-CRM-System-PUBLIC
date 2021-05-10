package com.ec.crm.Data;

import lombok.Data;

@Data
public class CustomerDetailInfo
{
	AllNotesForLeadDAO allNotes;
	AllActivitesForLeadDAO allActivities;
	LeadInformationAllTabDataList allNotedAndActivities;
}
