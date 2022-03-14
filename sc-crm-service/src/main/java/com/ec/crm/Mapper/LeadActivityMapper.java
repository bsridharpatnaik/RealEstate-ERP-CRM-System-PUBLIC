package com.ec.crm.Mapper;

import java.util.List;

import com.ec.crm.Enums.LeadStatusEnum;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.ec.crm.Data.LeadActivityOnLeadInformationDTO;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Service.LeadActivityService;

@Mapper(componentModel = "spring")
public abstract class LeadActivityMapper {
    @Autowired
    LeadActivityService leadActivityService;

    public abstract LeadActivityOnLeadInformationDTO mapLeadActivityToDTO(LeadActivity leadActivity) throws Exception;

    public abstract List<LeadActivityOnLeadInformationDTO> mapLeadActivitiesToDTOs(List<LeadActivity> leadActivity)
            throws Exception;

    @BeforeMapping
    public void setRevertable(LeadActivity la, @MappingTarget LeadActivityOnLeadInformationDTO laDto) throws Exception {
        laDto.setIsRevertable(leadActivityService.getRevertable(la.getLeadActivityId(), la.getLead().getLeadId()));
        laDto.setShowMoveToNegotiation(leadActivityService.getMoveToNegotiation(la));
    }

}
