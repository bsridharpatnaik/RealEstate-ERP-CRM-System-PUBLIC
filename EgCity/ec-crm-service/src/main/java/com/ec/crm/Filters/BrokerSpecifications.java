package com.ec.crm.Filters;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Model.Broker;
import com.ec.crm.Model.Broker_;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

import lombok.NonNull;

public final class BrokerSpecifications 
{
	static SpecificationsBuilder<Broker> specbldr = new SpecificationsBuilder<Broker>();
	
	public static Specification<Broker> fetchSpecification(FilterDataList brokerFilterDataList) 
	{
		Specification<Broker> specification = null;
		List<String> searchTerms = SpecificationsBuilder.fetchValueFromFilterList(brokerFilterDataList, "GlobalSearch");
		
		if(searchTerms != null && searchTerms.size()>0)
			specification = specbldr.specOrCondition(specbldr.whereDirectFieldContains(Broker_.BROKER_NAME, searchTerms),specbldr.whereDirectFieldContains(Broker_.BROKER_PHONENO, searchTerms));	
		return specification;
	}
}
