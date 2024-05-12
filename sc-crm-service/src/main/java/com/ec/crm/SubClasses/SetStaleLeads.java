package com.ec.crm.SubClasses;

import com.ec.crm.Data.ActivitiesForDashboard;
import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Model.ActivitiesStatsForDashboard;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

@NoArgsConstructor
public class SetStaleLeads implements Runnable {
    private CyclicBarrier barrier;
    private ActivitiesForDashboard dashboardPipelineReturnData;
    private List<ActivitiesStatsForDashboard> data;

    Logger log = LoggerFactory.getLogger(SetStaleLeads.class);

    public SetStaleLeads(ActivitiesForDashboard dashboardPipelineReturnData,
                         List<ActivitiesStatsForDashboard> data) {
        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.data = data;
    }

    @Override
    public void run() {
        try {
        log.info("Fetching stats for SetStaleLeads");
        dashboardPipelineReturnData
                .setStaleLeads(new MapForPipelineAndActivities(data.stream().filter(e -> e.getType().equals("stale")).mapToLong(i -> i.getCount()).sum()
                        , data.stream().filter(e -> e.getType().equals("stale")).collect(Collectors.toList())));
        log.info("Completed stats for tomorrow ");
        } catch (Exception e) {
            log.error("An error occurred in SetStaleLeads : " + e.getMessage());
        }
    }
}
