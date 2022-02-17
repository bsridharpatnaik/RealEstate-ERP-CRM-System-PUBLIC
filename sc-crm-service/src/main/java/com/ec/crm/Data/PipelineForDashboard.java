package com.ec.crm.Data;

import lombok.Data;

@Data
public class PipelineForDashboard {
    MapForPipelineAndActivities leadGenerated;
    MapForPipelineAndActivities prospectiveLeads;
    MapForPipelineAndActivities dealClosed;
    MapForPipelineAndActivities dealLost;
    MapForPipelineAndActivities dealLostReasons;
}
