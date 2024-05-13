package com.ec.crm.Service;

import java.util.List;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.*;
import com.ec.crm.Repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class DeletePostSalesRecordsService {
    @Autowired
    DealStructureRepo dsRepo;

    @Autowired
    PaymentScheduleRepo psRepo;

    @Autowired
    CustomerDocumentRepo cdsRepo;

    @Autowired
    PaymentReceivedRepo paymentReceivedRepo;

    @Autowired
    LeadActivityRepo laRepo;

    Logger log = LoggerFactory.getLogger(DeletePostSalesRecordsService.class);

    public void deleteAllForCustomer(Long customerId) {
        log.info("Initiated post sales cleanup for Customer - " + customerId);
        cleanUpDealStructures(customerId);
        cleanUpDocuments(customerId);
        cleanUpPayments(customerId);
        cleanUpPaymentActivities(customerId);
    }

    private void cleanUpPaymentActivities(Long customerId) {
        List<LeadActivity> laList = laRepo.fetchPendingPaymentActivitiesForLead(customerId);
        log.info("Identified " + laList.size() + " Payment Activities to be deleted.");
        for (LeadActivity la : laList) {
                la.setIsOpen(false);
                la.setClosedBy(404L);
                la.setClosingComment("Deal Cancelled");
                laRepo.save(la);
        }
    }

    private void cleanUpPayments(Long customerId) {
        List<PaymentReceived> prList = paymentReceivedRepo.getPaymentsListForLead(customerId);
        log.info("Identified " + prList.size() + " Payments to be deleted.");
        for (PaymentReceived pr : prList) {
            paymentReceivedRepo.softDelete(pr);
        }
    }

    private void cleanUpDocuments(Long customerId) {
        List<CustomerDocument> cdList = cdsRepo.findDocumentsForLead(customerId);
        log.info("Identified " + cdList.size() + " Documents to be deleted.");
        for (CustomerDocument cd : cdList)
            cdsRepo.softDelete(cd);
    }

    private void cleanUpDealStructures(Long customerId) {
        List<DealStructure> dsList = dsRepo.getDealStructureByLeadID(customerId);
        log.info("Identified " + dsList.size() + " Dealstructure records to be deleted.");
        for (DealStructure ds : dsList) {
            List<PaymentSchedule> psList = psRepo.getSchedulesForDeal(ds.getDealId());
            log.info("Identified " + psList.size() + " Payment Schedules records to be deleted.");
            for (PaymentSchedule ps : psList)
                psRepo.softDelete(ps);
            dsRepo.softDelete(ds);
        }
    }
}
