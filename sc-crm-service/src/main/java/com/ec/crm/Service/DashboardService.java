package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import com.ec.crm.Data.SalesFunnelDTO;
import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.Lead;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Strategy.IStrategy;
import com.ec.crm.Strategy.StrategyFactory;
import com.ec.crm.SubClasses.*;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.DashboardData;
import com.ec.crm.Data.PipelineAndActivitiesForDashboard;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.ConvertionRatioRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.StagnantStatsRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class DashboardService {

    @Autowired
    LeadActivityRepo lRepo;

    @Autowired
    StagnantStatsRepo stagnantStatsRepo;

    @Autowired
    ConvertionRatioRepo convertionRatioRepo;

    @Resource
    InstanceEnum currentInstance;

    @Autowired
    StrategyFactory strategyFactory;

    @Autowired
    LeadRepo leadRepo;

    @Resource
    private Map<Long, String> userIdNameMap;

    Logger log = LoggerFactory.getLogger(DashboardService.class);

    public PipelineAndActivitiesForDashboard customerpipeline(DashboardData payload) throws Exception {
        PipelineAndActivitiesForDashboard dashboardPipelineReturnData = new PipelineAndActivitiesForDashboard();
        List<LeadActivity> data = new ArrayList<LeadActivity>();
        Date fromdate = payload.getFromDate();
        Date todate = payload.getToDate();
        Date nextDate = DateUtils.addDays(todate, 1);
        log.info("fromdate : " + fromdate + " todate: " + nextDate);
        data = lRepo.getActivity(fromdate, nextDate);

        log.info("Fetching Stats");
        ExecutorService executors = Executors.newFixedThreadPool(8);
        CyclicBarrier barrier = new CyclicBarrier(8);

        executors.submit(new SetLeadGenerated(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetActivitiesCreated(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetPropertyVisit(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealClosed(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealLost(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetTodaysActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetPendingActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetUpcomingActivities(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealLostReasons(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        boolean flag = false;
        Date returnDateTime = new Date();
        while (flag == false) {
            if ((dashboardPipelineReturnData.getActivitiesCreated() == null
                    || dashboardPipelineReturnData.getActivitiesCreated() == null
                    || dashboardPipelineReturnData.getDealClosed() == null
                    || dashboardPipelineReturnData.getDealLost() == null
                    || dashboardPipelineReturnData.getLeadGenerated() == null
                    || dashboardPipelineReturnData.getPendingActivities() == null
                    || dashboardPipelineReturnData.getTodaysActivities() == null
                    || dashboardPipelineReturnData.getTotalPropertyVisit() == null
                    || dashboardPipelineReturnData.getUpcomingActivities() == null)
                    && (java.lang.Math.abs(returnDateTime.getTime() - new Date().getTime()) / 1000 < 30)) {
                log.info("Waiting for flag to be true. Current difference in time - "
                        + (returnDateTime.getTime() - new Date().getTime()) / 1000);
                flag = false;
            } else {
                log.info("Flag is true. Current difference in time - "
                        + (returnDateTime.getTime() - new Date().getTime()) / 1000);
                flag = true;
            }

        }
        return dashboardPipelineReturnData;

    }

    public List<ConversionRatio> conversionratio() {
        IStrategy strategy = strategyFactory.findStrategy(currentInstance);
        return strategy.fetchConversionRatio();
    }

    public List<StagnantStats> returnStagnantStats() {
        IStrategy strategy = strategyFactory.findStrategy(currentInstance);
        return strategy.returnStagnantStats();
    }

    public Map topperformer() {
        List<ConversionRatio> data = convertionRatioRepo.gettopperformer();
        Map returndata = new HashMap<>();
        if (data.size() > 0) {
            Long id = data.get(0).getUserId();
            Long propertyvisit = lRepo.getpropertyvisit(id, ReusableMethods.getStartOfMonth());
            returndata.put("username", data.get(0).getAsigneeName());
            returndata.put("leadsGenerated", data.get(0).getTotalcount());
            returndata.put("propertyVisits", propertyvisit);
            returndata.put("dealsClosed", data.get(0).getConvertedcount());
            returndata.put("inNegotiation", 2);
        } else {
            returndata.put("username", "NA");
            returndata.put("leadsGenerated", 0);
            returndata.put("propertyVisits", 0);
            returndata.put("dealsClosed", 0);
            returndata.put("inNegotiation", 0);
        }
        return returndata;
    }

    public List<SalesFunnelDTO> getSalesFunnel() {
        List<SalesFunnelDTO> salesFunnelData = new ArrayList<>();
        List<Lead> leadData = leadRepo.getOpenLeads();
        salesFunnelData.add(new SalesFunnelDTO(LeadStatusEnum.New_Lead, getLeadCountByStatus(leadData, LeadStatusEnum.New_Lead)));
        salesFunnelData.add(new SalesFunnelDTO(LeadStatusEnum.Visit_Scheduled, getLeadCountByStatus(leadData, LeadStatusEnum.Visit_Scheduled)));
        salesFunnelData.add(new SalesFunnelDTO(LeadStatusEnum.Visit_Completed, getLeadCountByStatus(leadData, LeadStatusEnum.Visit_Completed)));
        salesFunnelData.add(new SalesFunnelDTO(LeadStatusEnum.Negotiation, getLeadCountByStatus(leadData, LeadStatusEnum.Negotiation)));
        salesFunnelData.add(new SalesFunnelDTO(LeadStatusEnum.Deal_Closed, getLeadCountByStatus(leadData, LeadStatusEnum.Deal_Closed)));
        return salesFunnelData;
    }

    private long getLeadCountByStatus(List<Lead> leadData, LeadStatusEnum status) {
        long count = leadData.stream().filter(c -> c.getStatus().equals(status)).count();
        return count;
    }

}