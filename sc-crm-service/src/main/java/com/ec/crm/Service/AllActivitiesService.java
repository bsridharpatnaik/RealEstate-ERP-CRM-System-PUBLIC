package com.ec.crm.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import com.ec.crm.Model.RecentActivityForPipeline;
import com.ec.crm.Repository.RecentActivityForPipelineRepo;
import com.ec.crm.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    RecentActivityForPipelineRepo recentActivityForPipelineRepo;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public PipelineAllReturnDAO findFilteredDataForPipeline(FilterDataList leadFilterDataList, Pageable pageable)
            throws Exception {
        log.info("Invoked - findFilteredDataForPlanner");
        SpecificationsBuilder<Lead> specbldr = new SpecificationsBuilder<>();
        List<Lead> leads = new ArrayList<>();

        // check user. if not admin, apply default filters
        leadFilterDataList = utilService.addAssigneeToFilterData(leadFilterDataList);

        Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);

        Specification<Lead> internalSpec = (Root<Lead> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb
                .notEqual(root.get(Lead_.STATUS), Enum.valueOf(LeadStatusEnum.class, "Deal_Lost"));
        Specification<Lead> finalSpec = specbldr.specAndCondition(spec, internalSpec);
        leads = finalSpec!=null?lRepo.findAll(finalSpec):lRepo.findAll();
        System.out.println(" DFANBE = " + ThreadLocalStorage.getTenantName());
        return transformDataToPipelineMode(leads, ThreadLocalStorage.getTenantName());
    }

    private PipelineAllReturnDAO transformDataToPipelineMode(List<Lead> leads, String dbName) throws Exception {
        log.info("Invoked transformDataToPipelineMode");
        PipelineAllReturnDAO pipelineAllReturnDAO = new PipelineAllReturnDAO();
        pipelineAllReturnDAO.setDropdownData(populateDropdownService.fetchData("lead"));
        pipelineAllReturnDAO.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());

        // List to store futures for each async task
        List<CompletableFuture<PipelineWithTotalReturnDAO>> futures = new ArrayList<>();
        UserReturnData currentUser = (UserReturnData) request.getAttribute("currentUser");

        // Submit each task to the executor with try-catch blocks
        for (LeadStatusEnum leadStatus : LeadStatusEnum.getValuesForPipeline()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    ThreadLocalStorage.setTenantName(dbName);
                    return fetchPipelineDataFromActivityList(leads, leadStatus, currentUser);
                } catch (Exception e) {
                    throw new RuntimeException("Error fetching pipeline data for " + leadStatus, e);
                }
            }, executor));
        }

        // Wait for all async tasks to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        // Retrieve results from completed futures
        for (CompletableFuture<PipelineWithTotalReturnDAO> future : futures) {
            try {
                PipelineWithTotalReturnDAO data = future.get();
                if (data != null) {
                    switch (data.getLeadStatus()) {
                        case New_Lead:
                            pipelineAllReturnDAO.setLeadGeneration(data);
                            break;
                        case Negotiation:
                            pipelineAllReturnDAO.setNegotiation(data);
                            break;
                        case Visit_Scheduled:
                            pipelineAllReturnDAO.setPropertyVisitScheduled(data);
                            break;
                        case Visit_Completed:
                            pipelineAllReturnDAO.setPropertyVisitCompleted(data);
                            break;
                        case Deal_Closed:
                            pipelineAllReturnDAO.setDeal_close(data);
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving pipeline data from futures", e);
            }
        }
        return pipelineAllReturnDAO;
    }


    public PipelineWithTotalReturnDAO fetchPipelineDataFromActivityList(List<Lead> leads, LeadStatusEnum leadStatus, UserReturnData currentUser) throws Exception {
        List<Lead> filteredLeads = leads.stream().filter(Lead -> Lead.getStatus().equals(leadStatus))
                .collect(Collectors.toList());
        return transformToPipelineWithTotalReturnDAO(filteredLeads, currentUser, leadStatus);
    }

    private PipelineWithTotalReturnDAO transformToPipelineWithTotalReturnDAO(List<Lead> filteredLeads, UserReturnData currentUser, LeadStatusEnum leadStatus) throws Exception {
        log.info("Invoked transformToPipelineWithTotalReturnDAO");
        PipelineWithTotalReturnDAO PipelineWithTotalReturnDAO = new PipelineWithTotalReturnDAO();
        List<PipelineSingleReturnDTO> pipelineSingleReturnDTOList = new ArrayList<PipelineSingleReturnDTO>();

        for (Lead l : filteredLeads) {
                PipelineSingleReturnDTO pipelineSingleReturnDTO = new PipelineSingleReturnDTO();
                pipelineSingleReturnDTO.setIsOpen(l.getRecentIsOpen());
                pipelineSingleReturnDTO.setActivityDateTime(l.getRecentActivityDateTime());
                pipelineSingleReturnDTO.setLeadId(l.getLeadId());

                if (currentUser.getId().equals(l.getAsigneeId()) || utilService.isAdminOrManager(currentUser))
                    pipelineSingleReturnDTO.setMobileNumber((l.getPrimaryMobile()));
                else
                    pipelineSingleReturnDTO.setMobileNumber("******" + l.getPrimaryMobile().substring(7));

                pipelineSingleReturnDTO.setName(l.getCustomerName());
                pipelineSingleReturnDTO.setSentiment(l.getSentiment());
                //pipelineSingleReturnDTO.setActivityDateTime(l.getRecentActivityDateTime());
                pipelineSingleReturnDTO.setStagnantStatus(getStagnantStatus(l.getStagnantDaysCount()));
                //pipelineSingleReturnDTO.setIsOpen(l.getRecentActivityStatus());
                pipelineSingleReturnDTO.setAssignee(l.getAsigneeId());
                pipelineSingleReturnDTOList.add(pipelineSingleReturnDTO);
        }
        PipelineWithTotalReturnDAO.setLeads(pipelineSingleReturnDTOList);
        PipelineWithTotalReturnDAO.setTotalCount(filteredLeads.size());
        PipelineWithTotalReturnDAO.setLeadStatus(leadStatus); // Set the lead status
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

    @Transactional(rollbackFor = Exception.class)
    public void updateLeadActivityStatus(Long leadActivityId, Boolean status) {
        try {
            Optional<LeadActivity> leadActivityOpt = laRepo.findById(leadActivityId);
            if (leadActivityOpt.isPresent()) {
                LeadActivity leadActivity = leadActivityOpt.get();
                leadActivity.setIsOpen(status);
                laRepo.save(leadActivity);
            }
        } catch (Exception e) {
            // DO NOTHING - This is intentional
        }

    }
    public LeadActivityDropdownData getDropdownValues() throws Exception {
        LeadActivityDropdownData data = new LeadActivityDropdownData();
        data.setDropdownData(populateDropdownService.fetchData("lead"));
        data.setTypeAheadDataForGlobalSearch(leadService.fetchTypeAheadForLeadGlobalSearch());
        return data;
    }

    public PlannerAllReturnDAO findFilteredDataForPlanner(FilterDataList leadFilterDataList, Pageable pageable)
            throws Exception {
        log.info("Invoked - findFilteredData");

        // check user. if not admin, apply default filters
        leadFilterDataList = utilService.addAssigneeToFilterData(leadFilterDataList);

        //add today's date by default
        leadFilterDataList = utilService.addTodaysDateToFilterData(leadFilterDataList);

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
                                                                       String acivityType) throws Exception {
        log.info("Invoked PlannerWithTotalReturnDAO");
        List<LeadActivity> filteredActivities = activities.stream()
                .filter(LeadActivity -> LeadActivity.getActivityType().equals(ActivityTypeEnum.valueOf(acivityType)))
                .sorted(Comparator.comparing(LeadActivity::getActivityDateTime)).collect(Collectors.toList());
        return transformToPlannerWithTotalReturnDAO(filteredActivities);
    }

    private PlannerWithTotalReturnDAO transformToPlannerWithTotalReturnDAO(List<LeadActivity> filteredActivities) throws Exception {
        log.info("Invoked transformToPlannerWithTotalReturnDAO");
        UserReturnData currentUser = (UserReturnData) request.getAttribute("currentUser");
        PlannerWithTotalReturnDAO plannerWithTotalReturnDAO = new PlannerWithTotalReturnDAO();
        List<PlannerSingleReturnDAO> activities = new ArrayList<PlannerSingleReturnDAO>();
        for (LeadActivity leadActivity : filteredActivities) {
            String mobileNo = "";
            if (currentUser.getId().equals(leadActivity.getLead().getAsigneeId()) || utilService.isAdminOrManager(currentUser))
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
}
