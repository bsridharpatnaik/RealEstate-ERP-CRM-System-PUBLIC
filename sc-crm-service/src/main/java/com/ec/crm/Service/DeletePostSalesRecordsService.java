package com.ec.crm.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.crm.Model.CustomerDocument;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Repository.CustomerDocumentRepo;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.PaymentScheduleRepo;

@Service
@Transactional(rollbackFor = Exception.class)
public class DeletePostSalesRecordsService
{
	@Autowired
	DealStructureRepo dsRepo;

	@Autowired
	PaymentScheduleRepo psRepo;

	@Autowired
	CustomerDocumentRepo cdsRepo;

	Logger log = LoggerFactory.getLogger(DeletePostSalesRecordsService.class);

	public void deleteAllForCustomer(Long customerId)
	{
		log.info("Initiated post sales cleanup for Customer - " + customerId);
		cleanUpDealStructures(customerId);
		cleanUpDocuments(customerId);
	}

	private void cleanUpDocuments(Long customerId)
	{
		List<CustomerDocument> cdList = cdsRepo.findDocumentsForLead(customerId);
		log.info("Identified " + cdList.size() + " Documents to be deleted.");
		for (CustomerDocument cd : cdList)
			cdsRepo.softDelete(cd);
	}

	private void cleanUpDealStructures(Long customerId)
	{
		List<DealStructure> dsList = dsRepo.getDealStructureByLeadID(customerId);
		log.info("Identified " + dsList.size() + " Dealstructure records to be deleted.");
		for (DealStructure ds : dsList)
		{
			List<PaymentSchedule> psList = psRepo.getSchedulesForDeal(ds.getDealId());
			log.info("Identified " + psList.size() + " Payment Schedules records to be deleted.");
			for (PaymentSchedule ps : psList)
				psRepo.softDelete(ps);
			dsRepo.softDelete(ds);
		}
	}
}
