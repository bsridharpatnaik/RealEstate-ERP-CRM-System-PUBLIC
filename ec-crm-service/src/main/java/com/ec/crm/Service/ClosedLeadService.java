package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.ClosedLeadsListDTO;
import com.ec.crm.Data.DropdownForClosedLeads;
import com.ec.crm.Filters.ClosedLeadsSpecification;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.ClosedLeads;
import com.ec.crm.Repository.ClosedLeadsRepo;

@Service
public class ClosedLeadService
{

	@Autowired
	ClosedLeadsRepo clRepo;

	@Autowired
	PopulateDropdownService populateDropdownService;

	Logger log = LoggerFactory.getLogger(ClosedLeadService.class);

	public Page<ClosedLeadsListDTO> fetchAllClosedLeads(Pageable pageable, FilterDataList filterDataList)
			throws Exception
	{
		Specification<ClosedLeads> spec = ClosedLeadsSpecification.getSpecification(filterDataList);
		Page<ClosedLeadsListDTO> dtos = null;
		if (spec == null)
			dtos = clRepo.findAll(pageable).map(this::convertToDto);
		else
			dtos = clRepo.findAll(spec, pageable).map(this::convertToDto);
		return dtos;
	}

	private ClosedLeadsListDTO convertToDto(ClosedLeads o)
	{
		ClosedLeadsListDTO dto = new ClosedLeadsListDTO();
		dto.setAsigneeId(o.getAsigneeId());
		dto.setCustomerName(o.getCustomerName());
		dto.setLeadId(o.getLeadId());
		dto.setNextPaymentDate(null);
		dto.setPrimaryMobile(o.getPrimaryMobile());
		dto.setPropertyType(o.getPropertyType());
		return dto;
	}

	public DropdownForClosedLeads getDropDownValues() throws Exception
	{
		DropdownForClosedLeads dropdownValues = new DropdownForClosedLeads();
		dropdownValues.setDropdownData(populateDropdownService.fetchData("customer"));
		dropdownValues.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
		return dropdownValues;
	}

	private List<String> fetchTypeAheadForLeadGlobalSearch()
	{
		log.info("Invoked fetchTypeAheadForLeadGlobalSearch");
		List<String> typeAhead = new ArrayList<String>();
		typeAhead.addAll(clRepo.getLeadNames());
		typeAhead.addAll(clRepo.getLeadMobileNos());
		return typeAhead;
	}
}
