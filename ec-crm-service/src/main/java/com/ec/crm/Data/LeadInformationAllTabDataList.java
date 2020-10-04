package com.ec.crm.Data;

import java.util.ArrayList;

import lombok.Data;

@Data
public class LeadInformationAllTabDataList
{
	ArrayList<LeadInformationAllTabData> pendingActivities;
	ArrayList<LeadInformationAllTabData> pinnedNotes;
	ArrayList<LeadInformationAllTabData> pastNotesAndActivities;
}
