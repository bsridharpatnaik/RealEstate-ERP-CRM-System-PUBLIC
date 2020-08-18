package com.ec.crm;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import com.ec.crm.Data.LeadDAO;
import com.ec.crm.Data.LeadPageData;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Service.LeadActivityService;

@SpringBootApplication
public class CrmApplication extends SpringBootServletInitializer
{
	@Autowired
	LeadActivityService leadActivityService;

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}
	
	@Bean
	public ModelMapper leadToLeadActivityModelMapper() 
	{
		ModelMapper modelMapper = new ModelMapper();
		Converter<Lead,LeadPageData> leadActivity = new AbstractConverter<Lead, LeadPageData>() {
		      @Override
		      protected LeadPageData convert(Lead lead) 
		      {
		    	  LeadActivity leadActivity = leadActivityService.getRecentActivityByLeadId(lead.getLeadId());
		    	  LeadPageData leadPageData = new LeadPageData();
		    	  leadPageData.setAssigneeId(lead.getAsigneeId());
		    	  leadPageData.setLeadId(lead.getLeadId());
		    	  leadPageData.setLeadStatus(lead.getStatus());
		    	  leadPageData.setMobileNumber(lead.getPrimaryMobile());
		    	  leadPageData.setName(lead.getCustomerName());
		    	  leadPageData.setActivityDateTime(leadActivity.getActivityDateTime());
		    	  leadPageData.setActivityType(leadActivity.getActivityType());
		    	  leadPageData.setIsOpen(leadActivity.getIsOpen());
		    	  return leadPageData;
		      }
		    };

		    modelMapper.addConverter(leadActivity);
	    return modelMapper;
	} 
	
	@Bean
	public ModelMapper leadToLeadDAOModelMapper() 
	{
		ModelMapper modelMapper = new ModelMapper();
		Converter<Lead,LeadDAO> leadActivity = new AbstractConverter<Lead, LeadDAO>() 
		{
		      @Override
		      protected LeadDAO convert(Lead l) 
		      {
		    	  //LeadActivity leadActivity = leadActivityService.getRecentActivityByLeadId(lead.getLeadId());
		    	  LeadDAO leadDAO = new LeadDAO();
		    	  leadDAO.setAddress(
		    			  l.getAddress().getAddr_line1()==null?"":l.getAddress().getAddr_line1()+", "+
		    			  l.getAddress().getAddr_line2()==null?"":l.getAddress().getAddr_line2()+", "+
		    			  l.getAddress().getCity()==null?"":l.getAddress().getCity());
		    	  leadDAO.setAsigneeId(l.getAsigneeId());
		    	  leadDAO.setBroker(l.getBroker()==null?"":l.getBroker().getBrokerName());
		    	  leadDAO.setCreatorId(l.getCreatorId());
		    	  leadDAO.setCustomerName(l.getCustomerName());
		    	  leadDAO.setDateOfBirth(l.getDateOfBirth());
		    	  leadDAO.setEmailId(l.getEmailId());
		    	  leadDAO.setLeadId(l.getLeadId());
		    	  leadDAO.setOccupation(l.getOccupation());
		    	  leadDAO.setPrimaryMobile(l.getPrimaryMobile());
		    	  leadDAO.setPropertyType(l.getPropertyType());
		    	  leadDAO.setPurpose(l.getPurpose());
		    	  leadDAO.setSecondaryMobile(l.getSecondaryMobile());
		    	  leadDAO.setSource(l.getSource()==null?"":l.getSource().getSourceName());
		    	  leadDAO.setStatus(l.getStatus());
		    	  leadDAO.setStagnantDays(leadActivityService.getStagnantDaysByLeadId(l.getLeadId()));
		    	  return leadDAO;
		      }
		    };

		    modelMapper.addConverter(leadActivity);
	    return modelMapper;
	} 
	
	@Bean
	public ModelMapper modelMapper() 
	{
	    return new ModelMapper();
	}
}
