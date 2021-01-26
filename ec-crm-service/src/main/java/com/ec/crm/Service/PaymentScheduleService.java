package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ec.crm.Data.CreateScheduleData;
import com.ec.crm.Data.ScheduleReturnDAO;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.PaymentScheduleRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentScheduleService
{
	@Autowired
	DealStructureRepo dsRepo;

	@Autowired
	PaymentScheduleRepo psRepo;

	@Autowired
	LeadActivityService laService;

	@Autowired
	LeadActivityRepo laRepo;

	public ScheduleReturnDAO createSchedule(CreateScheduleData payload) throws Exception
	{
		validatePayload(payload, "create");
		PaymentSchedule ps = new PaymentSchedule();
		setFields(ps, payload);
		if (payload.getIsReceived().equals(false))
		{
			LeadActivity la = laService.createPaymentActivity(ps);
			ps.setLa(la);
		}
		psRepo.save(ps);
		return convertPStoPSDAO(ps);
	}

	public void deletePaymentSchedule(Long id) throws Exception
	{
		Optional<PaymentSchedule> psOpt = psRepo.findById(id);
		if (!psOpt.isPresent())
			throw new Exception("Payment Schedule not found with ID - " + id);
		PaymentSchedule ps = psOpt.get();
		if (ps.getLa() != null)
			laService.softDeleteLeadActivity(ps.getLa().getLeadActivityId());
		psRepo.softDeleteById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	public ScheduleReturnDAO updateSchedule(CreateScheduleData payload, Long id) throws Exception
	{
		validatePayload(payload, "update");
		if (!psRepo.existsById(id))
			throw new Exception("Payment Schedule not found with ID - " + id);
		PaymentSchedule ps = psRepo.findById(id).get();
		validateBeforeUpdate(ps, payload);
		if (ps.getLa() != null)
		{
			Date newDate = payload.getPaymentDate();
			newDate = ReusableMethods.setTimeTo11AM(newDate);
			ps.getLa().setActivityDateTime(newDate);
			if (!ps.getIsReceived().equals(payload.getIsReceived()))
			{
				if (payload.getIsReceived().equals(true))
					laService.deleteLeadActivity(ps.getLa().getLeadActivityId(), "Payment Received", (long) 404, false,
							"system");
				else
				{
					LeadActivity la = laService.createPaymentActivity(ps);
					ps.setLa(la);
				}
			}
		}
		setFields(ps, payload);

		psRepo.save(ps);
		return convertPStoPSDAO(ps);
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

	public List<ScheduleReturnDAO> getSchedulesForDeal(Long id) throws Exception
	{
		if (!dsRepo.existsById(id))
			throw new Exception("Deal Structure Not found with ID - " + id);

		List<PaymentSchedule> psList = psRepo.getSchedulesForDeal(id);
		return convertPSListtoPSDAOList(psList);
	}

	public ScheduleReturnDAO getPaymentSchedule(Long id) throws Exception
	{
		Optional<PaymentSchedule> psOpt = psRepo.findById(id);
		if (!psOpt.isPresent())
			throw new Exception("Payment Schedule not found with ID - " + id);
		return convertPStoPSDAO(psOpt.get());
	}

	private List<ScheduleReturnDAO> convertPSListtoPSDAOList(List<PaymentSchedule> psList)
	{
		List<ScheduleReturnDAO> returnList = new ArrayList<ScheduleReturnDAO>();
		for (PaymentSchedule ps : psList)
		{
			ScheduleReturnDAO dao = new ScheduleReturnDAO();
			dao = convertPStoPSDAO(ps);
			returnList.add(dao);
		}
		return returnList.stream().sorted(Comparator.comparing(ScheduleReturnDAO::getPaymentDate))
				.collect(Collectors.toList());
	}

	private ScheduleReturnDAO convertPStoPSDAO(PaymentSchedule ps)
	{
		ScheduleReturnDAO sDAO = new ScheduleReturnDAO();
		sDAO.setAmount(ps.getAmount() == null ? null : ps.getAmount());
		sDAO.setDealStructureId(ps.getDs() == null ? null : ps.getDs().getDealId());
		sDAO.setDetails(ps.getDetails() == null ? null : ps.getDetails());
		sDAO.setIsReceived(ps.getIsReceived() == null ? null : ps.getIsReceived());
		sDAO.setMode(ps.getMode() == null ? null : ps.getMode());
		sDAO.setPaymentDate(ps.getPaymentDate() == null ? null : ps.getPaymentDate());
		sDAO.setScheduleId(ps.getScheduleId() == null ? null : ps.getScheduleId());
		sDAO.setLeadActivityId(ps.getLa() == null ? null : ps.getLa().getLeadActivityId());
		return sDAO;
	}

	public void updateActivityForPaymentSchedule(Long oldLeadActivityId, Long newLeadActivityId2) throws Exception
	{
		try
		{
			List<PaymentSchedule> psList = psRepo.findByActivityID(oldLeadActivityId);
			if (psList.size() == 1)
			{
				LeadActivity la = laService.getSingleLeadActivity(newLeadActivityId2);
				PaymentSchedule ps = psList.get(0);
				ps.setPaymentDate(la.getActivityDateTime());
				ps.setLa(la);
				psRepo.save(ps);
			}
		} catch (Exception e)
		{
			throw new Exception("Something went wrong while rescheduling activity");
		}
	}
}
