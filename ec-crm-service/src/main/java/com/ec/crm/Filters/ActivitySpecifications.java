package com.ec.crm.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Model.Address_;
import com.ec.crm.Model.Broker_;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.LeadActivity_;
import com.ec.crm.Model.Lead_;
import com.ec.crm.Model.Source_;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

public class ActivitySpecifications 
{
static SpecificationsBuilder<LeadActivity> specbldr = new SpecificationsBuilder<LeadActivity>();
	
	public static Specification<LeadActivity> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> name = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"name");
		List<String> primarymobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"primaryMobile");
		List<String> purpose = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"purpose");
		List<String> address = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"address");
		List<String> occupation = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"occupation");
		List<String> broker = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"broker");
		List<String> source = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"source");
		List<String> propertytype = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"propertyType");
		List<String> sentiment = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"sentiment");
		List<String> assignee = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"assignee");
		List<String> createdStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"createdStartDate");
		List<String> createdEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"createdEndDate");
		List<String> leadStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"leadStatus");
		List<String> activityType = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"activityType");
		List<String> activityStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"activityStatus");
		List<String> activityStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"activityStartDate");
		List<String> activityEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"activityEndDate");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		Specification<LeadActivity> finalSpec = null;
		
		if(name != null && name.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.customerName.getName(),name));	
	
		if(primarymobile != null && primarymobile.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.PRIMARY_MOBILE,primarymobile));
		
		if(purpose != null && purpose.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.PURPOSE,purpose));
		
		if(address != null && address.size()>0)
		{	
			Specification<LeadActivity> addressSpec = specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.ADDRESS, Address_.ADDR_LINE1, address)
					.or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.ADDRESS, Address_.ADDR_LINE2, address))
					.or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.ADDRESS, Address_.CITY, address))
					.or(specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.ADDRESS, Address_.PINCODE, address));
			finalSpec = specbldr.specAndCondition(finalSpec,addressSpec);
		
		}
		
		if(globalSearch != null && globalSearch.size()>0)
		{	
			Specification<LeadActivity> globalSearchSpec = specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.CUSTOMER_NAME, globalSearch)
					.or(specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.PRIMARY_MOBILE,globalSearch));
					
			finalSpec = specbldr.specAndCondition(finalSpec,globalSearchSpec);
		
		}
		
		if(occupation != null && occupation.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.OCCUPATION,occupation));
		
		if(broker != null && broker.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.BROKER,Broker_.BROKER_NAME,broker));
		
		if(source != null && source.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereGrandChildFieldContains(LeadActivity_.LEAD,Lead_.SOURCE,Source_.SOURCE_NAME,source));
		
		if(propertytype != null && propertytype.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.PROPERTY_TYPE,propertytype));
		
		if(sentiment != null && sentiment.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.SENTIMENT,sentiment));
		
		if(assignee != null && assignee.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildLongFieldContains(LeadActivity_.LEAD,Lead_.ASIGNEE_ID,assignee));
		
		if(createdStartDate != null && createdStartDate.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldDateGreaterThan(LeadActivity_.LEAD,Lead_.CREATED, createdStartDate));	
		
		if(createdEndDate != null && createdEndDate.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldDateLessThan(LeadActivity_.LEAD,Lead_.CREATED, createdEndDate));
		
		if(leadStatus != null && leadStatus.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(LeadActivity_.LEAD,Lead_.STATUS,leadStatus));
		
		if(activityType != null && activityType.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(LeadActivity_.ACTIVITY_TYPE,activityType));
		
		if(activityStatus != null && activityStatus.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(LeadActivity_.IS_OPEN,activityStatus));
		
		if(activityStartDate != null && activityStartDate.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateGreaterThan(LeadActivity_.ACTIVITY_DATE_TIME,activityStartDate));
		
		if(activityEndDate != null && activityEndDate.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateLessThan(LeadActivity_.ACTIVITY_DATE_TIME,activityEndDate));
		return finalSpec;
	}
}
