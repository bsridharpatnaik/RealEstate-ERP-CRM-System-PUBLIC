package com.ec.crm.Data;

import lombok.Data;

@Data
public class PipelineAndActivitiesForDashboard 
{
	MapForPipelineAndActivities leadGenerated;
	MapForPipelineAndActivities activitiesCreated;
	MapForPipelineAndActivities totalPropertyVisit;
	MapForPipelineAndActivities dealClosed;
	MapForPipelineAndActivities dealLost;
	MapForPipelineAndActivities todaysActivities;
	MapForPipelineAndActivities pendingActivities;
	MapForPipelineAndActivities upcomingActivities;
	MapForPipelineAndActivities dealLostReasons;
}
