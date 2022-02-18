package com.ec.crm.Data;

import lombok.Data;

@Data
public class PipelineForDashboard {
    MapForPipelineAndActivities activitiesCreated;
    MapForPipelineAndActivities leadGenerated;
    MapForPipelineAndActivities dealClosed;
    MapForPipelineAndActivities dealLost;
    MapForPipelineAndActivities dealLostReasons;
}
