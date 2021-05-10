package com.ec.crm.SubClasses;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Enums.*;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.ReusableClasses.ReusableMethods;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

@NoArgsConstructor
public class SetDealLostReasons implements Runnable {
    private CyclicBarrier barrier;
    private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
    private List<LeadActivity> data;
    Map<Long, String> idNameMap;
    InstanceEnum instance;
    Logger log = LoggerFactory.getLogger(SetDealLostReasons.class);

    public SetDealLostReasons(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
                              List<LeadActivity> data, Map<Long, String> idNameMap, InstanceEnum instance1) {

        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
        this.idNameMap = idNameMap;
        this.instance = instance1;
    }

    @Override
    public void run() {

        log.info("Fetching stats for SetUpcomingActivities");

            dashboardPipelineReturnData
                    .setDealLostReasons(
                            new MapForPipelineAndActivities(
                                    data.stream()
                                            .filter(c -> (c.getActivityType().equals(ActivityTypeEnum.Deal_Lost)
                                                            && c.getLead().getStatus().equals(LeadStatusEnum.Deal_Lost)))
                                            .count(),
                                    data.stream()
                                            .filter(c -> (c.getActivityType().equals(ActivityTypeEnum.Deal_Lost)
                                                && c.getLead().getStatus().equals(LeadStatusEnum.Deal_Lost)))
                                            .collect(Collectors.groupingBy(c ->
                                            {
                                                return c.getDealLostReason()==null?DealLostReasonEnum.Empty:c.getDealLostReason();
                                            }, Collectors.counting()))));
        log.info("Completed stats for SetUpcomingActivities");
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
