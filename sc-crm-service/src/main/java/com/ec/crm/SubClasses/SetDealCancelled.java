package com.ec.crm.SubClasses;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineForDashboard;
import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
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
public class SetDealCancelled implements Runnable {
    private CyclicBarrier barrier;
    private PipelineForDashboard dashboardPipelineReturnData;
    private List<LeadActivity> data;
    Map<Long, String> idNameMap;
    InstanceEnum instance;
    Logger log = LoggerFactory.getLogger(SetDealCancelled.class);

    public SetDealCancelled(CyclicBarrier barrier, PipelineForDashboard dashboardPipelineReturnData,
                            List<LeadActivity> data, Map<Long, String> idNameMap, InstanceEnum instance1) {

        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
        this.idNameMap = idNameMap;
        this.instance = instance1;
    }

    @Override
    public void run() {

        log.info("Fetching stats for SetDealCancelled");
        if (instance.equals(InstanceEnum.egcity))
            dashboardPipelineReturnData.setDealCancelled(new MapForPipelineAndActivities(
                    data.stream().filter(c -> c.getActivityType().name().equals("Deal_Cancelled")).count(),
                    data.stream().filter(c -> c.getActivityType().name().equals("Deal_Cancelled")).collect(Collectors.groupingBy(c ->
                    {
                        try {
                            return idNameMap.get(c.getLead().getAsigneeId());
                        } catch (Exception e) { 
                            log.error(e.getMessage());
                            e.printStackTrace();
                        }
                        return null;
                    }, Collectors.counting()))));
        if (instance.equals(InstanceEnum.suncity))
            dashboardPipelineReturnData.setDealCancelled(new MapForPipelineAndActivities(
                    data.stream().filter(c -> c.getActivityType().name().equals("Deal_Cancelled")).count(),
                    data.stream().filter(c -> c.getActivityType().name().equals("Deal_Cancelled")).collect(Collectors.groupingBy(c ->
                    {
                        try {
                            return c.getLead().getPropertyType()==null? PropertyTypeEnum.Empty:c.getLead().getPropertyType();
                        } catch (Exception e) { 
                            log.error(e.getMessage());
                            e.printStackTrace();
                        }
                        return null;
                    }, Collectors.counting()))));
        log.info("Completed stats for SetDealCancelled");
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
