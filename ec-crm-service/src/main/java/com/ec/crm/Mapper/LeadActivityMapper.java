package com.ec.crm.Mapper;

import java.util.List;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.ec.crm.Data.LeadActivityOnLeadInformationDTO;
import com.ec.crm.Model.LeadActivity;

@Mapper(componentModel="spring")
public abstract class LeadActivityMapper 
{
	public abstract LeadActivityOnLeadInformationDTO mapLeadActivityToDTO(LeadActivity leadActivity);
	public abstract List<LeadActivityOnLeadInformationDTO> mapLeadActivitiesToDTOs(List<LeadActivity> leadActivity);
	
	@BeforeMapping
    public void setRevertable(LeadActivity la, @MappingTarget LeadActivityOnLeadInformationDTO laDto) 
	{
		laDto.setIsRevertable(true);
    }
	
}
