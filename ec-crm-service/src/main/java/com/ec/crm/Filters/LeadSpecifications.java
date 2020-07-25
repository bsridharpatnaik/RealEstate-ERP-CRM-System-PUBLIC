package com.ec.crm.Filters;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.ReusableClasses.SpecificationsBuilder;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Lead_;

import lombok.NonNull;

public final class LeadSpecifications 
{
	static SpecificationsBuilder<Lead> specbldr = new SpecificationsBuilder<Lead>();
	
	public static Specification<Lead> getSpecification(FilterDataList filterDataList)
	{
		List<String> name = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"name");
		List<String> primarymobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"primarymobile");
		List<String> secondarymobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"secondarymobile");
		List<String> emailId = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"emailId");
		List<String> occupation = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"occupation");
		List<String> dob = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"dob");
		
		Specification<Lead> finalSpec = null;
		
		if(name != null && name.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.customerName.getName(),name));	
	
		
		if(primarymobile != null && primarymobile.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.primaryMobile.getName(),primarymobile));	
	
		if(secondarymobile != null && secondarymobile.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.secondaryMobile.getName(),secondarymobile));	
	
		if(emailId != null && emailId.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.emailId.getName(),emailId));	
	
		if(occupation != null && occupation.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.occupation.getName(),occupation));	
	
		if(dob != null && dob.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Lead_.dateOfBirth.getName(),dob));	
	
		
		return finalSpec;
	}

	
}
