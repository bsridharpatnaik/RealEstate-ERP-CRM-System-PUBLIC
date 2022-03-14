package com.ec.crm.SubClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import com.ec.crm.Data.MapForDashboardStatsDAO;
import com.ec.crm.Data.PipelineForDashboard;
import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.crm.Data.MapForPipelineAndActivities;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.LeadActivity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SetLeadGenerated implements Runnable {
    private CyclicBarrier barrier;
    private PipelineForDashboard dashboardPipelineReturnData;
    private List<LeadActivity> data;
    Map<Long, String> idNameMap;
    InstanceEnum instance;
    Logger log = LoggerFactory.getLogger(SetLeadGenerated.class);

    public SetLeadGenerated(CyclicBarrier barrier, PipelineForDashboard dashboardPipelineReturnData,
                            List<LeadActivity> data, Map<Long, String> idNameMap, InstanceEnum instance1) {

        this.dashboardPipelineReturnData = dashboardPipelineReturnData;
        this.barrier = barrier;
        this.data = data;
        this.idNameMap = idNameMap;
        this.instance = instance1;
    }

    @Override
    public void run() {

        log.info("Fetching stats for Lead Generated");
        if (instance.equals(InstanceEnum.egcity))
            dashboardPipelineReturnData.setLeadGenerated(
                    new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() == 404).count(),
                            data.stream().filter(c -> c.getCreatorId() == 404).collect(Collectors.groupingBy(c ->
                            {

                                try {

                                    return idNameMap.get(c.getLead().getAsigneeId());
                                } catch (Exception e) {
                                    
                                    log.error(e.getMessage());
                                    e.printStackTrace();
                                    return null;
                                }

                            }, Collectors.counting()))));
        if (instance.equals(InstanceEnum.suncity))
            dashboardPipelineReturnData.setLeadGenerated(
                    new MapForPipelineAndActivities(data.stream().filter(c -> c.getCreatorId() == 404).count(),
                            data.stream().filter(c -> c.getCreatorId() == 404).collect(Collectors.groupingBy(c ->
                            {

                                try {
                                    return c.getLead().getPropertyType()==null? PropertyTypeEnum.Empty:c.getLead().getPropertyType();
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                    e.printStackTrace();
                                    return null;
                                }

                            }, Collectors.counting()))));
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
