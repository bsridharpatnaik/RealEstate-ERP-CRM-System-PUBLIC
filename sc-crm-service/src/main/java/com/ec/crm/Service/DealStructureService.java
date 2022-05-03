
package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.CreateDealStructureDTO;
import com.ec.crm.Data.DealStructureDAO;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Repository.ClosedLeadsRepo;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.PropertyNameRepo;
import com.ec.crm.Repository.PropertyTypeRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class DealStructureService {

    @Autowired
    DealStructureRepo dealStructureRepo;

    @Autowired
    ClosedLeadsRepo clRepo;

    @Autowired
    PropertyTypeRepo ptRepo;

    @Autowired
    PropertyNameRepo pnRepo;

    @Autowired
    PaymentScheduleService psService;

    public List<IdNameProjections> getPropertyTypes() {
        return ptRepo.findIdAndNames();
    }

    public DealStructureDAO createDealStructure(CreateDealStructureDTO payload) throws Exception {
        validatePayload(payload, "create");
        DealStructure ds = new DealStructure();
        setFields(ds, payload);
        return convertDStoDSDAO(dealStructureRepo.save(ds));
    }

    private void setFields(DealStructure ds, CreateDealStructureDTO payload) {
        ds.setDealAmount(payload.getDealAmount());
        ds.setLead(clRepo.findById(payload.getLeadId()).get());
        ds.setDetails(payload.getDetails());
        ds.setBookingDate(payload.getBookingDate());
        ds.setLoanRequired(payload.getLoanRequired());
        ds.setSupplementAmount(payload.getSupplementAmount());
        ds.setCustomerStatus(payload.getCustomerStatus());
        if (payload.getLoanRequired()) {
            ds.setBankName(payload.getBankName());
            ds.setLoanAmount(payload.getLoanAmount());
            ds.setLoanStatus(payload.getLoanStatus());
            ds.setCustomerAmount(payload.getDealAmount() + payload.getSupplementAmount() - payload.getLoanAmount());
        } else {
            ds.setBankName(null);
            ds.setLoanAmount(null);
            ds.setLoanStatus(null);
            ds.setCustomerAmount(payload.getDealAmount() + payload.getSupplementAmount());
        }
        ds.setPropertyName(pnRepo.findById(payload.getPropertyId()).get());
        ds.setPropertyType(ptRepo.findById(payload.getPropertyTypeId()).get());
    }

    private void validatePayload(CreateDealStructureDTO payload, String operation) throws Exception {
        List<String> missingFields = new ArrayList<String>();

        if (payload.getLeadId() == null)
            missingFields.add("Lead");

        if (payload.getBookingDate() == null)
            missingFields.add("Booking Date");

        if (payload.getPropertyId() == null)
            missingFields.add("Property");

        if (payload.getPropertyTypeId() == null)
            missingFields.add("Property Type");

        if (payload.getDealAmount() == null)
            missingFields.add("Deal Amount");

        if (payload.getSupplementAmount() == null)
            payload.setSupplementAmount((double) 0);

        if (payload.getLoanRequired() == null)
            missingFields.add("Loan Required");

        if (payload.getLoanRequired()) {
            if (payload.getLoanAmount() == null)
                missingFields.add("Loan Amount");
            if (payload.getBankName() == null)
                missingFields.add("Bank Name");
            if (payload.getLoanStatus() == null)
                missingFields.add("Loan Status");
            if (payload.getCustomerStatus() == null)
                missingFields.add("Customer Status");
            if (payload.getLoanAmount() > payload.getDealAmount())
                throw new Exception("Loan amount cannot be greater than Deal Amount");
        }

        if (missingFields.size() > 0)
            throw new Exception("Required fields missing - " + String.join(",", missingFields));

        if (!clRepo.existsById(payload.getLeadId()))
            throw new Exception("Customer not found with ID - " + payload.getLeadId());

        if (!pnRepo.existsById(payload.getPropertyId()))
            throw new Exception("Property not found with ID - " + payload.getPropertyId());

        if (!ptRepo.existsById(payload.getPropertyTypeId()))
            throw new Exception("Property Type not found with ID - " + payload.getPropertyTypeId());

        if (payload.getDetails().length() > 149)
            throw new Exception("Details should be less than 150 characters");

        if (operation.equals("create")) {
            if (dealStructureRepo.countByPropertyName(payload.getPropertyId()) > 0)
                throw new Exception("Deal structure already added for property.");
        }
    }

    public List<IdNameProjections> getPropertiesList(Long id) {
        List<IdNameProjections> list = ptRepo.fetchAvailableProperties(id);
        return list;
    }

    public List<DealStructureDAO> getDealStructuresForLead(Long id) throws Exception {
        if (!clRepo.existsById(id))
            throw new Exception("Customer not found with ID -" + id);
        List<DealStructure> dsList = dealStructureRepo.getDealStructureByLeadID(id);
        return convertDSListToDAOList(dsList);
    }

    public DealStructureDAO updateDealStructure(CreateDealStructureDTO payload, Long id) throws Exception {
        if (!dealStructureRepo.existsById(id))
            throw new Exception("Deal structure not found by ID - " + id);

        DealStructure ds = dealStructureRepo.findById(id).get();
        validatePayloadBeforeEdit(payload, ds);
        setFields(ds, payload);
        return convertDStoDSDAO(dealStructureRepo.save(ds));

    }

    private void validatePayloadBeforeEdit(CreateDealStructureDTO payload, DealStructure ds) throws Exception {
        if (!payload.getLeadId().equals(ds.getLead().getLeadId()))
            throw new Exception("Lead cannot be updated while updating deal structure");

        if (dealStructureRepo.countByPropertyName(payload.getPropertyId()) > 0
                && !payload.getPropertyId().equals(ds.getPropertyName().getPropertyNameId()))
            throw new Exception("Deal structure already added for property - " + payload.getPropertyId());
    }

    public void deleteDealStructure(Long id) throws Exception {
        if (!dealStructureRepo.existsById(id))
            throw new Exception("Deal structure not found by ID - " + id);

        psService.deletePaymentSchedulesForDealStructure(id);
        //TODO deletePaymentForDealStructure();
        dealStructureRepo.softDeleteById(id);
    }

    public DealStructureDAO getDealStructure(Long id) throws Exception {
        Optional<DealStructure> dsOpt = dealStructureRepo.findById(id);
        if (!dsOpt.isPresent())
            throw new Exception("Deal structure not found by ID - " + id);
        return convertDStoDSDAO(dsOpt.get());
    }

    private List<DealStructureDAO> convertDSListToDAOList(List<DealStructure> dsList) throws Exception {
        List<DealStructureDAO> daoList = new ArrayList<DealStructureDAO>();
        for (DealStructure ds : dsList) {
            daoList.add(convertDStoDSDAO(ds));
        }
        return daoList.stream().sorted(Comparator.comparing(DealStructureDAO::getBookingDate))
                .collect(Collectors.toList());
    }

    private DealStructureDAO convertDStoDSDAO(DealStructure ds) throws Exception {
        DealStructureDAO dao = new DealStructureDAO();
        dao.setBankName(ds.getBankName() == null ? "" : ds.getBankName());
        dao.setBookingDate(ds.getBookingDate());
        dao.setCustomerAmount(ds.getCustomerAmount() == null ? 0 : ds.getCustomerAmount());
        dao.setDealAmount(ds.getDealAmount());
        dao.setDealId(ds.getDealId());
        dao.setDetails(ds.getDetails());
        dao.setLeadId(ds.getLead().getLeadId());
        dao.setLoanAmount(ds.getLoanAmount() == null ? 0 : ds.getLoanAmount());
        dao.setLoanRequired(ds.getLoanRequired());
        dao.setLoanStatus(ds.getLoanStatus() == null ? null : ds.getLoanStatus());
        dao.setCustomerStatus(ds.getCustomerStatus() == null ? null : ds.getCustomerStatus());
        dao.setPropertyName(ds.getPropertyName().getName());
        dao.setPropertyNameId(ds.getPropertyName().getPropertyNameId());
        dao.setPropertyType(ds.getPropertyType().getPropertyType());
        dao.setPropertytypeId(ds.getPropertyType().getPropertyTypeId());
        dao.setSchedules(psService.getSchedulesForDeal(ds.getDealId()));
        dao.setSupplementAmount(ds.getSupplementAmount() == null ? 0 : ds.getSupplementAmount());
        dao.setTotalPendingBank(ds.getTotalPendingBank() == null ? 0 : ds.getTotalPendingBank());
        dao.setTotalPendingCustomer(ds.getTotalPendingCustomer() == null ? 0 : ds.getTotalPendingCustomer());
        dao.setTotalReceivedBank(ds.getTotalReceivedBank() == null ? 0 : ds.getTotalReceivedBank());
        dao.setTotalReceivedCustomer(ds.getTotalReceivedCustomer() == null ? 0 : ds.getTotalReceivedCustomer());
        dao.setTenPercentOfTotalAmount(ds.getTenPercentOfTotalAmount());
        dao.setRemainingOfTenPercentTotalAmount(ds.getRemainingOfTenPercentTotalAmount());
        return dao;
    }

    public Boolean getDealStructureStatusForLead(Long id) {
        List<DealStructure> dsList = dealStructureRepo.getDealStructureByLeadID(id);
        if (dsList.size() > 0)
            return true;
        else
            return false;
    }
}
