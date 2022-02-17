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
public class SetLiveLeads implements Runnable {
    private CyclicBarrier barrier;
    private ActivitiesForDashboard dashboardPipelineReturnData;
    private List<ActivitiesStatsForDashboard> data;

    Logger log = LoggerFactory.getLogger(SetLiveLeads.class);

    public SetLiveLeads(CyclicBarrier barrier, ActivitiesForDashboard dashboardPipelineReturnData,
                        List<ActivitiesStatsForDashboard> data) {
        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
    }

    @Override
    public void run() {

        log.info("Fetching stats for Set Live Leads");
        dashboardPipelineReturnData
                .setLiveLeads(new MapForPipelineAndActivities(data.stream().filter(e -> e.getType().equals("live")).mapToLong(i -> i.getCount()).sum()
                        , data.stream().filter(e -> e.getType().equals("live")).collect(Collectors.toList())));
        log.info("Completed stats for Live Leads");
        try {
            barrier.await();
        } catch (InterruptedException e) {

            e.printStackTrace();
        } catch (BrokenBarrierException e) {

            e.printStackTrace();
        }
    }
}
