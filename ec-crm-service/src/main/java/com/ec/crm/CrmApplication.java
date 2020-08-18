package com.ec.crm;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

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
	public ModelMapper modelMapper() 
	{
	    return new ModelMapper();
	}
}
