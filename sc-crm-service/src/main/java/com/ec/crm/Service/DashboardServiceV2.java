package com.ec.crm.Service;

import com.ec.crm.Data.*;
import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.*;
import com.ec.crm.Repository.*;
import com.ec.crm.ReusableClasses.ReusableMethods;
import com.ec.crm.Strategy.IStrategy;
import com.ec.crm.Strategy.StrategyFactory;
import com.ec.crm.SubClasses.*;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional(rollbackFor = Exception.class)
public class DashboardServiceV2 {

    @Autowired
    LeadActivityRepo lRepo;

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

    @Autowired
    ActivitiesStatsForDashboardRepo activitiesStatsForDashboardRepo;

    Logger log = LoggerFactory.getLogger(DashboardServiceV2.class);

    public PipelineForDashboard getPipelineForDashboard(DashboardData payload) throws Exception {

        PipelineForDashboard dashboardPipelineReturnData = new PipelineForDashboard();
        List<LeadActivity> data = new ArrayList<LeadActivity>();
        Date fromdate = payload.getFromDate();
        Date todate = payload.getToDate();
        Date nextDate = DateUtils.addDays(todate, 1);
        log.info("fromdate : " + fromdate + " todate: " + nextDate);
        data = lRepo.getActivity(fromdate, nextDate);

        log.info("Fetching Stats");
        ExecutorService executors = Executors.newFixedThreadPool(5);
        CyclicBarrier barrier = new CyclicBarrier(5);
        executors.submit(new SetActivitiesCreated(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetLeadGenerated(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealClosed(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealLost(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        executors.submit(new SetDealLostReasons(barrier, dashboardPipelineReturnData, data, userIdNameMap, currentInstance));
        boolean flag = false;
        Date returnDateTime = new Date();
        while (flag == false) {
            if ((
                    dashboardPipelineReturnData.getDealClosed() == null
                            || dashboardPipelineReturnData.getDealLost() == null
                            || dashboardPipelineReturnData.getLeadGenerated() == null
                            || dashboardPipelineReturnData.getDealLostReasons() == null)
                    && (Math.abs(returnDateTime.getTime() - new Date().getTime()) / 1000 < 5)) {
                flag = false;
            } else {
                log.info("Flag is true. Current difference in time - "
                        + (returnDateTime.getTime() - new Date().getTime()) / 1000);
                flag = true;
            }

        }
        return dashboardPipelineReturnData;
    }

    public ActivitiesForDashboard getActivitesForDashboard() throws Exception {
        ActivitiesForDashboard dashboardPipelineReturnData = new ActivitiesForDashboard();
        List<ActivitiesStatsForDashboard> data = activitiesStatsForDashboardRepo.findAll();

        log.info("Fetching Activity Stats");
        ExecutorService executors = Executors.newFixedThreadPool(6);
        CyclicBarrier barrier = new CyclicBarrier(6);
        executors.submit(new SetTodaysActivities(barrier, dashboardPipelineReturnData, data));
        executors.submit(new SetPendingActivities(barrier, dashboardPipelineReturnData, data));
        executors.submit(new SetUpcomingActivities(barrier, dashboardPipelineReturnData, data));
        executors.submit(new SetLiveLeads(barrier, dashboardPipelineReturnData, data));
        executors.submit(new SetProspectLeads(barrier, dashboardPipelineReturnData, data));
        executors.submit(new SetTomorrowActivities(barrier, dashboardPipelineReturnData, data));
        boolean flag = false;
        Date returnDateTime = new Date();
        while (flag == false) {
            if ((dashboardPipelineReturnData.getTodaysActivities() == null
                    || dashboardPipelineReturnData.getPendingActivities() == null
                    || dashboardPipelineReturnData.getUpcomingActivities() == null
                    || dashboardPipelineReturnData.getProspectiveLeads() == null
                    || dashboardPipelineReturnData.getLiveLeads() == null
                    || dashboardPipelineReturnData.getTomorrowsActivities() == null
            )
                    && (Math.abs(returnDateTime.getTime() - new Date().getTime()) / 1000 < 5)) {
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