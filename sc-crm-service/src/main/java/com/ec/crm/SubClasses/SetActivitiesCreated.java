package com.ec.crm.SubClasses;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetActivitiesCreated implements Runnable {
    private CyclicBarrier barrier;
    private PipelineAndActivitiesForDashboard dashboardPipelineReturnData;
    private List<LeadActivity> data;
    Map<Long, String> idNameMap;
    InstanceEnum instance;
    Logger log = LoggerFactory.getLogger(SetActivitiesCreated.class);

    public SetActivitiesCreated(CyclicBarrier barrier, PipelineAndActivitiesForDashboard dashboardPipelineReturnData,
                                List<LeadActivity> data, Map<Long, String> idNameMap, InstanceEnum instance1) {

        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
        this.idNameMap = idNameMap;
        this.instance = instance1;
    }

    @Override
    public void run() {

        try {
            log.info("Fetching stats for SetActivitiesCreated");
            if (instance.equals(InstanceEnum.egcity))
                dashboardPipelineReturnData.setActivitiesCreated(
                        new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() != 405).count(),
                                data.stream().filter(c -> c.getCreatorId() != 405).collect(Collectors.groupingBy(c ->
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
                dashboardPipelineReturnData.setActivitiesCreated(
                        new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() != 405).count(),
                                data.stream().filter(c -> c.getCreatorId() != 405).collect(Collectors.groupingBy(c ->
                                {
                                    try {
                                        return c.getLead().getPropertyType()==null? PropertyTypeEnum.Empty:c.getLead().getPropertyType();
                                    } catch (Exception e) {
                                        log.error(e.getMessage());
                                        e.printStackTrace();
                                    }
                                    return null;
                                }, Collectors.counting()))));
            log.info("Completed stats for SetActivitiesCreated");
        } finally {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {

                e.printStackTrace();
            }
        }
    }
}
