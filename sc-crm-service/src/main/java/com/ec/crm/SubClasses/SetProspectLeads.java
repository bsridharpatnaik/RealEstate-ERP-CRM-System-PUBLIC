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

    public SetProspectLeads(CyclicBarrier barrier, ActivitiesForDashboard dashboardPipelineReturnData,
                            List<ActivitiesStatsForDashboard> data) {
        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
    }

    @Override
    public void run() {

        log.info("Fetching stats for Prospect Lead");
        dashboardPipelineReturnData
                .setProspectiveLeads(new MapForPipelineAndActivities(data.stream().filter(e -> e.getType().equals("prospect")).mapToLong(i -> i.getCount()).sum()
                        , data.stream().filter(e -> e.getType().equals("prospect")).collect(Collectors.toList())));
        log.info("Completed stats for Lead Generated");
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
