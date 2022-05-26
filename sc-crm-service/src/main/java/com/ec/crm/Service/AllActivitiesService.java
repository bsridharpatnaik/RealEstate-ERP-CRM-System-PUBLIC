package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.LeadActivityDropdownData;
import com.ec.crm.Data.PipelineAllReturnDAO;
import com.ec.crm.Data.PipelineSingleReturnDTO;
import com.ec.crm.Data.PipelineWithTotalReturnDAO;
import com.ec.crm.Data.PlannerAllReturnDAO;
import com.ec.crm.Data.PlannerSingleReturnDAO;
import com.ec.crm.Data.PlannerWithTotalReturnDAO;
import com.ec.crm.Enums.StagnatedEnum;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Filters.ActivitySpecifications;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.Lead_;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class AllActivitiesService {
    @Autowired
    LeadActivityRepo laRepo;

    @Autowired
    LeadRepo lRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    LeadService leadService;

    Logger log = LoggerFactory.getLogger(AllActivitiesService.class);

    @Autowired
    HttpServletRequest request;

    @Autowired
    LeadActivityService leadActivityService;

    @Autowired
    UtilService utilService;

    public PlannerAllReturnDAO findFilteredDataForPlanner(FilterDataList leadFilterDataList, Pageable pageable)
            throws Exception {
        log.info("Invoked - findFilteredData");

        // check user. if not admin, apply default filters
        leadFilterDataList = utilService.addAssigneeToFilterData(leadFilterDataList);

        List<LeadActivity> activities = new ArrayList<LeadActivity>();

        Specification<LeadActivity> spec = ActivitySpecifications.getSpecification(leadFilterDataList);
        if (spec != null)
            activities = laRepo.findAll(spec);
        else
            activities = laRepo.findAll();

        PlannerAllReturnDAO returnData = transformDataToPlannerMode(activities);
        return returnData;
    }

    private PlannerAllReturnDAO transformDataToPlannerMode(List<LeadActivity> activities) throws Exception {
        log.info("Invoked transformDataToPlannerMode");
        PlannerAllReturnDAO activitiesList = new PlannerAllReturnDAO();
        activitiesList.setCall(fetchPlannerDataFromActivityList(activities, "Call"));
        activitiesList.setMeeting(fetchPlannerDataFromActivityList(activities, "Meeting"));
        activitiesList.setProperty_visit(fetchPlannerDataFromActivityList(activities, "Property_Visit"));
        activitiesList.setDeal_close(fetchPlannerDataFromActivityList(activities, "Deal_Close"));
        activitiesList.setReminder(fetchPlannerDataFromActivityList(activities, "Reminder"));
        activitiesList.setPayment(fetchPlannerDataFromActivityList(activities, "Payment"));
        activitiesList.setMessage(fetchPlannerDataFromActivityList(activities, "Message"));
        activitiesList.setEmail(fetchPlannerDataFromActivityList(activities, "Email"));
        activitiesList.setDeal_lost(fetchPlannerDataFromActivityList(activities, "Deal_Lost"));
        activitiesList.setDropdownData(populateDropdownService.fetchData("lead"));
        activitiesList.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());
        return activitiesList;
    }

    private PlannerWithTotalReturnDAO fetchPlannerDataFromActivityList(List<LeadActivity> activities,
                                                                       String acivityType) {
        log.info("Invoked PlannerWithTotalReturnDAO");
        List<LeadActivity> filteredActivities = activities.stream()
                .filter(LeadActivity -> LeadActivity.getActivityType().equals(ActivityTypeEnum.valueOf(acivityType)))
                .sorted(Comparator.comparing(LeadActivity::getActivityDateTime)).collect(Collectors.toList());
        return transformToPlannerWithTotalReturnDAO(filteredActivities);
    }

    private PlannerWithTotalReturnDAO transformToPlannerWithTotalReturnDAO(List<LeadActivity> filteredActivities) {
        log.info("Invoked transformToPlannerWithTotalReturnDAO");
        UserReturnData currentUser = (UserReturnData) request.getAttribute("currentUser");
        PlannerWithTotalReturnDAO plannerWithTotalReturnDAO = new PlannerWithTotalReturnDAO();
        List<PlannerSingleReturnDAO> activities = new ArrayList<PlannerSingleReturnDAO>();
        for (LeadActivity leadActivity : filteredActivities) {
            String mobileNo = "";
            if (currentUser.getId().equals(leadActivity.getLead().getAsigneeId())
                    || currentUser.getRoles().contains("CRM-Manager") || currentUser.getRoles().contains("admin"))
                mobileNo = leadActivity.getLead().getPrimaryMobile();
            else
                mobileNo = "******" + leadActivity.getLead().getPrimaryMobile().substring(7);
            activities.add(new PlannerSingleReturnDAO(leadActivity.getLead().getLeadId(),
                    leadActivity.getLead().getCustomerName(), mobileNo, leadActivity.getIsOpen(),
                    leadActivity.getActivityDateTime(), leadActivity.getLead().getAsigneeId(),
                    leadActivity.getLead().getStatus()));
        }
        plannerWithTotalReturnDAO.setActivities(activities);
        plannerWithTotalReturnDAO.setTotalActivities(activities.size());
        return plannerWithTotalReturnDAO;
    }

    public LeadActivityDropdownData getDropdownValues() throws Exception {
        LeadActivityDropdownData data = new LeadActivityDropdownData();
        data.setDropdownData(populateDropdownService.fetchData("lead"));
        data.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());
        return data;
    }

    public PipelineAllReturnDAO findFilteredDataForPipeline(FilterDataList leadFilterDataList, Pageable pageable)
            throws Exception {
        log.info("Invoked - findFilteredDataForPlanner");
        SpecificationsBuilder<Lead> specbldr = new SpecificationsBuilder<Lead>();
        List<Lead> leads = new ArrayList<Lead>();

        // check user. if not admin, apply default filters
        leadFilterDataList = utilService.addAssigneeToFilterData(leadFilterDataList);

        Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);

        Specification<Lead> internalSpec = (Root<Lead> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .notEqual(root.get(Lead_.STATUS), Enum.valueOf(LeadStatusEnum.class, "Deal_Lost"));
        Specification<Lead> finalSpec = specbldr.specAndCondition(spec, internalSpec);

        if (finalSpec != null)
            leads = lRepo.findAll(finalSpec);
        else
            leads = lRepo.findAll();

        System.out.println("Size ----" + leads.size());
        PipelineAllReturnDAO returnData = transformDataToPipelineMode(leads);

        return returnData;
    }

    private PipelineAllReturnDAO transformDataToPipelineMode(List<Lead> leads) throws Exception {
        log.info("Invoked transformDataToPipelineMode");
        PipelineAllReturnDAO pipelineAllReturnDAO = new PipelineAllReturnDAO();
        pipelineAllReturnDAO.setDropdownData(populateDropdownService.fetchData("lead"));
        pipelineAllReturnDAO.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());

        HashMap<Long, LeadActivity> leadRecentActivityMapping = fetchRecentActivityForAllLeads(leads);

        pipelineAllReturnDAO.setLeadGeneration(
                fetchPipelineDataFromActivityList(leads, LeadStatusEnum.New_Lead, leadRecentActivityMapping));
        pipelineAllReturnDAO.setNegotiation(
                fetchPipelineDataFromActivityList(leads, LeadStatusEnum.Negotiation, leadRecentActivityMapping));
        pipelineAllReturnDAO.setPropertyVisitScheduled(
                fetchPipelineDataFromActivityList(leads, LeadStatusEnum.Visit_Scheduled, leadRecentActivityMapping));
        pipelineAllReturnDAO.setPropertyVisitCompleted(
                fetchPipelineDataFromActivityList(leads, LeadStatusEnum.Visit_Completed, leadRecentActivityMapping));
        pipelineAllReturnDAO.setDeal_close(
                fetchPipelineDataFromActivityList(leads, LeadStatusEnum.Deal_Closed, leadRecentActivityMapping));

        return pipelineAllReturnDAO;
    }

    private HashMap<Long, LeadActivity> fetchRecentActivityForAllLeads(List<Lead> leads) {
        HashMap<Long, LeadActivity> leadRecentActivityMapping = new HashMap<>();
        for (Lead l : leads) {
            LeadActivity recentActivity = leadActivityService.getRecentActivityByLead(l);
            leadRecentActivityMapping.put(l.getLeadId(), recentActivity);
        }
        return leadRecentActivityMapping;
    }

    public PipelineWithTotalReturnDAO fetchPipelineDataFromActivityList(List<Lead> leads, LeadStatusEnum leadStatus,
                                                                        HashMap<Long, LeadActivity> leadRecentActivityMapping) throws Exception {
        List<Lead> filteredLeads = leads.stream().filter(Lead -> Lead.getStatus().equals(leadStatus))
                .collect(Collectors.toList());
        return transformToPipelineWithTotalReturnDAO(filteredLeads, leadRecentActivityMapping);
    }

    private PipelineWithTotalReturnDAO transformToPipelineWithTotalReturnDAO(List<Lead> filteredLeads,
                                                                             HashMap<Long, LeadActivity> leadRecentActivityMapping) throws Exception {
        UserReturnData currentUser = (UserReturnData) request.getAttribute("currentUser");
        log.info("Invoked transformToPipelineWithTotalReturnDAO");

        PipelineWithTotalReturnDAO PipelineWithTotalReturnDAO = new PipelineWithTotalReturnDAO();
        List<PipelineSingleReturnDTO> pipelineSingleReturnDTOList = new ArrayList<PipelineSingleReturnDTO>();

        for (Lead l : filteredLeads) {
            LeadActivity recentActivity = leadRecentActivityMapping.get(l.getLeadId());
            PipelineSingleReturnDTO pipelineSingleReturnDTO = new PipelineSingleReturnDTO();
            pipelineSingleReturnDTO.setLeadId(l.getLeadId());

            if (currentUser.getId().equals(l.getAsigneeId()) || currentUser.getRoles().contains("CRM-Manager")
                    || currentUser.getRoles().contains("admin"))
                pipelineSingleReturnDTO.setMobileNumber((l.getPrimaryMobile()));
            else
                pipelineSingleReturnDTO.setMobileNumber("******" + l.getPrimaryMobile().substring(7));

            pipelineSingleReturnDTO.setName(l.getCustomerName());
            pipelineSingleReturnDTO.setSentiment(l.getSentiment());
            pipelineSingleReturnDTO.setActivityDateTime(recentActivity.getActivityDateTime());
            pipelineSingleReturnDTO.setStagnantStatus(getStagnantStatus(l.getStagnantDaysCount()));
            pipelineSingleReturnDTO.setIsOpen(recentActivity.getIsOpen());
            pipelineSingleReturnDTO.setAssignee(l.getAsigneeId());
            pipelineSingleReturnDTOList.add(pipelineSingleReturnDTO);

        }
        PipelineWithTotalReturnDAO.setLeads(pipelineSingleReturnDTOList);
        PipelineWithTotalReturnDAO.setTotalCount(filteredLeads.size());
        return PipelineWithTotalReturnDAO;
    }

    private StagnatedEnum getStagnantStatus(Long noOfDay) throws Exception {
        StagnatedEnum stagnatedStatus = StagnatedEnum.NoColor;

        if (noOfDay >= 10 && noOfDay < 20)
            stagnatedStatus = StagnatedEnum.GREEN;
        else if (noOfDay >= 20 && noOfDay < 30)
            stagnatedStatus = StagnatedEnum.ORANGE;
        else if (noOfDay >= 30)
            stagnatedStatus = StagnatedEnum.RED;
        return stagnatedStatus;
    }

    @Transactional
    public void updateLeadActivityStatus(Long leadActivityId, Boolean status){
        try{

        }

    }
}
