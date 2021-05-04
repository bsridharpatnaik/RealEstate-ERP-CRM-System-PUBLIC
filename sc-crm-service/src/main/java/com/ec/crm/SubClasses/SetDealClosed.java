package com.ec.crm.SubClasses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.ec.crm.Enums.InstanceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetDealClosed implements Runnable {
    private CyclicBarrier barrier;
    private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
    private List<LeadActivity> data;
    Map<Long, String> idNameMap;
    InstanceEnum instance;
    Logger log = LoggerFactory.getLogger(SetDealClosed.class);

    public SetDealClosed(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
                         List<LeadActivity> data, Map<Long, String> idNameMap, InstanceEnum instance1) {

        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
        this.idNameMap = idNameMap;
        this.instance = instance1;
    }

    @Override
    public void run() {
        log.info("Fetching stats for SetDealClosed");
        if (instance.equals(InstanceEnum.egcity))
            dashboardPipelineReturnData.setDealClosed(new MapForPipelineAndActivities(
                    data.stream().filter(c -> c.getActivityType().name() == "Deal_Close").count(),
                    data.stream().filter(c -> c.getActivityType().name() == "Deal_Close").collect(Collectors.groupingBy(c ->
                    {
                        try {
                            return idNameMap.get(c.getLead().getAsigneeId());
                        } catch (Exception e) { // TODO Auto-generated catch block
                            log.error(e.getMessage());
                            e.printStackTrace();
                        }
                        return null;
                    }, Collectors.counting()))));
        if (instance.equals(InstanceEnum.suncity))
            dashboardPipelineReturnData.setDealClosed(new MapForPipelineAndActivities(
                    data.stream().filter(c -> c.getActivityType().name() == "Deal_Close").count(),
                    data.stream().filter(c -> c.getActivityType().name() == "Deal_Close").collect(Collectors.groupingBy(c ->
                    {
                        try {
                            return c.getLead().getPropertyType();
                        } catch (Exception e) { // TODO Auto-generated catch block
                            log.error(e.getMessage());
                            e.printStackTrace();
                        }
                        return null;
                    }, Collectors.counting()))));
        log.info("Completed stats for SetDealClosed");
        try {
            barrier.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
