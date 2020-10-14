package com.ec.crm.Data;

import java.util.List;

import lombok.Data;

@Data
public class LeadInformationAllTabDataList
{
	List<LeadInformationAllTabData> pendingActivities;
	List<LeadInformationAllTabData> pinnedNotes;
	List<LeadInformationAllTabData> pastNotesAndActivities;
}
