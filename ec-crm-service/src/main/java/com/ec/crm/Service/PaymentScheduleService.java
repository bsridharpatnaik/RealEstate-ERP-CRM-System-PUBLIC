package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.CreateScheduleData;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.PaymentScheduleRepo;

@Service
public class PaymentScheduleService
{
	@Autowired
	DealStructureRepo dsRepo;

	@Autowired
	PaymentScheduleRepo psRepo;

	public PaymentSchedule createSchedule(CreateScheduleData payload) throws Exception
	{
		validatePayload(payload, "create");
		PaymentSchedule ps = new PaymentSchedule();
		setFields(ps, payload);
		psRepo.save(ps);
		return ps;
	}

	public void deletePaymentSchedule(Long id) throws Exception
	{
		if (!psRepo.existsById(id))
			throw new Exception("Payment Schedule not found with ID - " + id);
		psRepo.softDeleteById(id);

	}

	public PaymentSchedule updateSchedule(CreateScheduleData payload, Long id) throws Exception
	{
		validatePayload(payload, "update");
		if (!psRepo.existsById(id))
			throw new Exception("Payment Schedule not found with ID - " + id);
		PaymentSchedule ps = psRepo.findById(id).get();
		validateBeforeUpdate(ps, payload);
		setFields(ps, payload);
		psRepo.save(ps);
		return ps;
	}

	private void validateBeforeUpdate(PaymentSchedule ps, CreateScheduleData payload) throws Exception
	{
		if (!ps.getDs().getDealId().equals(payload.getDealStructureId()))
			throw new Exception("Cannot modify Deal Structure while updating schedule");
	}

	private void setFields(PaymentSchedule ps, CreateScheduleData payload)
	{
		DealStructure ds = dsRepo.findById(payload.getDealStructureId()).get();
		ps.setAmount(payload.getAmount());
		ps.setDetails(payload.getDetails());
		ps.setDs(ds);
		ps.setIsReceived(payload.getIsReceived());
		ps.setMode(payload.getMode());
		ps.setPaymentDate(payload.getPaymentDate());
	}

	private void validatePayload(CreateScheduleData payload, String operation) throws Exception
	{
		List<String> missingFields = new ArrayList<String>();
		if (payload.getDealStructureId() == null)
			missingFields.add("Deal Struture ID");
		if (payload.getAmount() == null)
			missingFields.add("Amount");
		if (payload.getDetails() == null)
			missingFields.add("Details");
		if (payload.getMode() == null)
			missingFields.add("Mode");
		if (payload.getPaymentDate() == null)
			missingFields.add("Payment Date");
		if (payload.getIsReceived() == null)
			missingFields.add("Received Status");

		if (missingFields.size() > 0)
			throw new Exception("Required fields missing - " + String.join(",", missingFields));

		if (!dsRepo.existsById(payload.getDealStructureId()))
			throw new Exception("Deal Structure Not found with ID - " + payload.getDealStructureId());

		if (payload.getDetails().length() > 150)
			throw new Exception("Details should be less than 150 chracters");

	}

	public List<PaymentSchedule> getSchedulesForDeal(Long id) throws Exception
	{
		if (!dsRepo.existsById(id))
			throw new Exception("Deal Structure Not found with ID - " + id);

		List<PaymentSchedule> psList = psRepo.getSchedulesForDeal(id);
		return psList;
	}
}
