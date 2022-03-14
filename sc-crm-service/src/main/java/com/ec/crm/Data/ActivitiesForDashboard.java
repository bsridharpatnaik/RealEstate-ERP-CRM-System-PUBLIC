package com.ec.crm.Data;

import lombok.Data;

@Data
public class ActivitiesForDashboard {
    MapForPipelineAndActivities liveLeads;
    MapForPipelineAndActivities prospectiveLeads;
    MapForPipelineAndActivities todaysActivities;
    MapForPipelineAndActivities tomorrowsActivities;
    MapForPipelineAndActivities pendingActivities;
    MapForPipelineAndActivities upcomingActivities;
}

