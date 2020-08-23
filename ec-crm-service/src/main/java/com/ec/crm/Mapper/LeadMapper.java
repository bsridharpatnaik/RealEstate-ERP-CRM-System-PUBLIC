package com.ec.crm.Mapper;

import org.mapstruct.Mapper;

import com.ec.crm.Data.LeadDTOforLeadInformation;
import com.ec.crm.Model.Lead;

@Mapper(componentModel="spring")
public interface LeadMapper 
{
	LeadDTOforLeadInformation mapLeadForLeadInformation(Lead lead);
}
