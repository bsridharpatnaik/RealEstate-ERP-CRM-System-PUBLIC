package com.ec.crm.SubClasses;

import com.ec.crm.Data.ActivitiesForDashboard;
import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineForDashboard;
import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Model.ActivitiesStatsForDashboard;
import com.ec.crm.Model.LeadActivity;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

@NoArgsConstructor
public class SetProspectLeads implements Runnable {
    private CyclicBarrier barrier;
    private ActivitiesForDashboard dashboardPipelineReturnData;
    private List<ActivitiesStatsForDashboard> data;
    Logger log = LoggerFactory.getLogger(SetProspectLeads.class);

    public SetProspectLeads(ActivitiesForDashboard dashboardPipelineReturnData,
                            List<ActivitiesStatsForDashboard> data) {
        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            log.info("Fetching stats for Prospect Lead");
            dashboardPipelineReturnData
                    .setProspectiveLeads(new MapForPipelineAndActivities(data.stream().filter(e -> e.getType().equals("prospect")).mapToLong(i -> i.getCount()).sum()
                            , data.stream().filter(e -> e.getType().equals("prospect")).collect(Collectors.toList())));
            log.info("Completed stats for Lead Generated");
        } catch (Exception e) {
            log.error("An error occurred in Set Prospect Leads: " + e.getMessage());
        }
    }
}
