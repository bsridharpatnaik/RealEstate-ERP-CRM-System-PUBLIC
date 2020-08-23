package com.ec.crm.Mapper;

import org.mapstruct.Mapper;

import com.ec.crm.Data.LeadActivityDTO;
import com.ec.crm.Model.LeadActivity;

@Mapper(componentModel="spring")
public interface LeadActivityMapper 
{
	LeadActivityDTO mapLeadActivityToDTO(LeadActivity leadActivity);
}
