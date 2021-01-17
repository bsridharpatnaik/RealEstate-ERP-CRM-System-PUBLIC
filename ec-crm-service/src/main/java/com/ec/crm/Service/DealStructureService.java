
package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.CreateDealStructureDTO;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Repository.ClosedLeadsRepo;
import com.ec.crm.Repository.DealStructureRepo;
import com.ec.crm.Repository.PropertyNameRepo;
import com.ec.crm.Repository.PropertyTypeRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

@Service
public class DealStructureService
{

	@Autowired
	DealStructureRepo dealStructureRepo;

	@Autowired
	ClosedLeadsRepo clRepo;

	@Autowired
	PropertyTypeRepo ptRepo;

	@Autowired
	PropertyNameRepo pnRepo;

	public List<IdNameProjections> getPropertyTypes()
	{
		return ptRepo.findIdAndNames();
	}

	public DealStructure createDealStructure(CreateDealStructureDTO payload) throws Exception
	{
		validatePayload(payload, "create");
		DealStructure ds = new DealStructure();
		setFields(ds, payload);
		return dealStructureRepo.save(ds);
	}

	private void setFields(DealStructure ds, CreateDealStructureDTO payload)
	{
		ds.setAmount(payload.getAmount());
		ds.setBookingDate(payload.getBookingDate());
		ds.setDetails(payload.getDetails());
		ds.setLead(clRepo.findById(payload.getLeadId()).get());
		ds.setMode(payload.getMode());
		ds.setPhase(payload.getPhase());
		ds.setPropertyName(pnRepo.findById(payload.getPropertyId()).get());
		ds.setPropertyType(ptRepo.findById(payload.getPropertyTypeId()).get());
	}

	private void validatePayload(CreateDealStructureDTO payload, String operation) throws Exception
	{
		List<String> missingFields = new ArrayList<String>();

		if (payload.getLeadId() == null)
			missingFields.add("Lead");

		if (payload.getBookingDate() == null)
			missingFields.add("Booking Date");

		if (payload.getPropertyId() == null)
			missingFields.add("Property");

		if (payload.getPropertyTypeId() == null)
			missingFields.add("Property Type");

		if (payload.getMode() == null)
			missingFields.add("Payment Mode");

		if (payload.getAmount() == null)
			missingFields.add("Booking Amount");

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

		if (dealStructureRepo.countByPropertyName(payload.getPropertyId()) > 0 && !operation.equals("update"))
			throw new Exception("Deal structure already added for property.");
	}

	public List<IdNameProjections> getPropertiesList(Long id)
	{
		List<IdNameProjections> list = ptRepo.fetchAvailableProperties(id);
		return list;
	}
}
