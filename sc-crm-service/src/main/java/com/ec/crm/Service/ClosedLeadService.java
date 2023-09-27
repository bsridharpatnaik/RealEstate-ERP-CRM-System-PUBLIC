package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import com.ec.crm.Model.PaymentReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.AllActivitesForLeadDAO;
import com.ec.crm.Data.AllNotesForLeadDAO;
import com.ec.crm.Data.ClosedLeadStatusDAO;
import com.ec.crm.Data.ClosedLeadsListDTO;
import com.ec.crm.Data.CustomerDetailInfo;
import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Data.LeadDAO;
import com.ec.crm.Filters.ClosedLeadsSpecification;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.ClosedLeads;
import com.ec.crm.Repository.ClosedLeadsRepo;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ClosedLeadService {

    @Autowired
    ClosedLeadsRepo clRepo;

    @Autowired
    PopulateDropdownService populateDropdownService;

    Logger log = LoggerFactory.getLogger(ClosedLeadService.class);

    @Autowired
    LeadService leadService;

    @Autowired
    LeadActivityService leadActivityService;

    @Autowired
    NoteService noteService;

    @Autowired
    CustomerDocumentsService cdService;

    @Autowired
    DealStructureService dsService;

    @Autowired
    PaymentReceivedService paymentReceivedService;

    @Autowired
    AuthorizationService authorizationService;;

    public Page<ClosedLeadsListDTO> fetchAllClosedLeads(Pageable pageable, FilterDataList filterDataList)
            throws Exception {
        Specification<ClosedLeads> spec = ClosedLeadsSpecification.getSpecification(filterDataList);
        Page<ClosedLeadsListDTO> dtos = null;
        if (spec == null)
            dtos = clRepo.findAll(pageable).map(this::convertToDto);
        else
            dtos = clRepo.findAll(spec, pageable).map(this::convertToDto);
        return dtos;
    }

    private ClosedLeadsListDTO convertToDto(ClosedLeads o) {
        ClosedLeadsListDTO dto = new ClosedLeadsListDTO();
        dto.setAsigneeId(o.getAsigneeId());
        dto.setCustomerName(o.getCustomerName());
        dto.setLeadId(o.getLeadId());
        dto.setNextPaymentDate(null);
        dto.setPrimaryMobile(o.getPrimaryMobile());
        dto.setPropertyType(o.getPropertyType());
        dto.setLoanStatus(o.getLoanStatus());
        dto.setCustomerStatus(o.getCustomerStatus());
        return dto;
    }

    public DropdownForClosedLeads getDropDownValues() throws Exception {
        DropdownForClosedLeads dropdownValues = new DropdownForClosedLeads();
        dropdownValues.setDropdownData(populateDropdownService.fetchData("customer"));
        dropdownValues.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
        return dropdownValues;
    }

    private List<String> fetchTypeAheadForLeadGlobalSearch() {
        log.info("Invoked fetchTypeAheadForLeadGlobalSearch");
        List<String> typeAhead = new ArrayList<String>();
        typeAhead.addAll(clRepo.getLeadNames());
        typeAhead.addAll(clRepo.getLeadMobileNos());
        return typeAhead;
    }

    public CustomerDetailInfo getCustomerDetailedInfo(Long id) throws Exception {

        CustomerDetailInfo customerDetailedInfo = new CustomerDetailInfo();
        AllNotesForLeadDAO allNotes = noteService.getAllNotesForLead(id);
        AllActivitesForLeadDAO allActivities = leadActivityService.getAllActivitiesForLead(id);
        customerDetailedInfo.setAllActivities(allActivities);
        customerDetailedInfo.setAllNotedAndActivities(leadService.transformDataForAllTab(allNotes, allActivities));
        customerDetailedInfo.setAllNotes(allNotes);
        return customerDetailedInfo;
    }

    public LeadDAO getCustomerDetails(Long id) throws Exception {
        if (!clRepo.existsById(id))
            throw new Exception("Customer not found with ID -" + id);

        LeadDAO l = new LeadDAO();
        convertLeadToLeadDAO(clRepo.findById(id).get(), l);
        authorizationService.exitIfNotAllowed(l);
        return l;
    }

    private void convertLeadToLeadDAO(ClosedLeads lead, LeadDAO l) {
        l.setAddr_line1(lead.getAddress().getAddr_line1() == null ? null : lead.getAddress().getAddr_line1());
        l.setAddr_line2(lead.getAddress().getAddr_line2() == null ? null : lead.getAddress().getAddr_line2());
        l.setAsigneeId(lead.getAsigneeId());
        l.setAssigneeUserId(lead.getAsigneeId());
        l.setBroker(lead.getBroker() == null ? null : lead.getBroker().getBrokerName());
        l.setCity(lead.getAddress().getCity() == null ? null : lead.getAddress().getCity());
        l.setCreatorId(lead.getCreatorId() == null ? null : lead.getCreatorId());
        l.setCustomerName(lead.getCustomerName());
        l.setDateOfBirth(lead.getDateOfBirth() == null ? null : lead.getDateOfBirth());
        l.setEmailId(lead.getEmailId() == null ? null : lead.getEmailId());
        l.setLastActivityModifiedDate(
                lead.getLastActivityModifiedDate() == null ? null : lead.getLastActivityModifiedDate());
        l.setLeadId(lead.getLeadId());
        l.setOccupation(lead.getOccupation() == null ? null : lead.getOccupation());
        l.setPincode(lead.getAddress().getPincode() == null ? null : lead.getAddress().getPincode());
        l.setPrimaryMobile((lead.getPrimaryMobile()));
        l.setPropertyType(lead.getPropertyType() == null ? null : lead.getPropertyType());
        l.setPurpose(lead.getPurpose() == null ? null : lead.getPurpose());
        l.setSecondaryMobile(lead.getSecondaryMobile());
        l.setSentiment(lead.getSentiment() == null ? null : lead.getSentiment());
        l.setSource(lead.getSource() == null ? null : lead.getSource().getSourceName());
        l.setStagnantDaysCount(lead.getStagnantDaysCount() == null ? null : lead.getStagnantDaysCount());
        l.setStatus(lead.getStatus());
    }

    public ClosedLeadStatusDAO getStatusForStepper(Long id) throws Exception {
        ClosedLeadStatusDAO returnData = new ClosedLeadStatusDAO();
        returnData.setActivities(true);
        returnData.setHandover(false);
        returnData.setLoanDetails(false);
        returnData.setDealStructure(dsService.getDealStructureStatusForLead(id));
        returnData.setDocuments(cdService.getDocumentStatusByLead(id));
        returnData.setPayment(paymentReceivedService.getPaymentStepperStatus(id));
        return returnData;
    }

}
