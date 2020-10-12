package com.ec.crm.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Model.Address_;
import com.ec.crm.Model.Broker_;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Lead_;
import com.ec.crm.Model.Source_;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

public final class LeadSpecifications
{
	static SpecificationsBuilder<Lead> specbldr = new SpecificationsBuilder<Lead>();

	public static Specification<Lead> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> name = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
		List<String> mobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "mobile");
		List<String> purpose = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "purpose");
		List<String> address = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "address");
		List<String> occupation = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "occupation");
		List<String> broker = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "broker");
		List<String> source = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "source");
		List<String> propertytype = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "propertyType");
		List<String> sentiment = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "sentiment");
		List<String> assignee = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "assignee");
		List<String> createdBy = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "createdBy");
		List<String> createdStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"createdStartDate");
		List<String> createdEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "createdEndDate");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
		List<String> leadStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "leadStatus");
		List<String> stagnantStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "stagnantStatus");
		Specification<Lead> finalSpec = null;

		if (name != null && name.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Lead_.customerName.getName(), name));

		if (mobile != null && mobile.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Lead_.PRIMARY_MOBILE, mobile)
							.or(specbldr.whereDirectFieldContains(Lead_.SECONDARY_MOBILE, mobile)));

		if (purpose != null && purpose.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(Lead_.PURPOSE, purpose));

		if (address != null && address.size() > 0)
		{
			Specification<Lead> addressSpec = specbldr
					.whereChildFieldContains(Lead_.ADDRESS, Address_.ADDR_LINE1, address)
					.or(specbldr.whereChildFieldContains(Lead_.ADDRESS, Address_.ADDR_LINE2, address))
					.or(specbldr.whereChildFieldContains(Lead_.ADDRESS, Address_.CITY, address))
					.or(specbldr.whereChildFieldContains(Lead_.ADDRESS, Address_.PINCODE, address));
			finalSpec = specbldr.specAndCondition(finalSpec, addressSpec);

		}
		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<Lead> globalSearchSpec = specbldr.whereDirectFieldContains(Lead_.CUSTOMER_NAME, globalSearch)
					.or(specbldr.whereDirectFieldContains(Lead_.PRIMARY_MOBILE, globalSearch));

			finalSpec = specbldr.specAndCondition(finalSpec, globalSearchSpec);

		}

		if (occupation != null && occupation.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Lead_.OCCUPATION, occupation));

		if (broker != null && broker.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(Lead_.BROKER, Broker_.BROKER_NAME, broker));

		if (source != null && source.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(Lead_.SOURCE, Source_.SOURCE_NAME, source));

		if (propertytype != null && propertytype.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereEnumFieldEquals(Lead_.PROPERTY_TYPE, propertytype, PropertyTypeEnum.class));

		if (sentiment != null && sentiment.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereEnumFieldEquals(Lead_.SENTIMENT, sentiment, SentimentEnum.class));

		if (createdBy != null && createdBy.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectAssigneeContains(Lead_.CREATOR_ID, createdBy));

		if (assignee != null && assignee.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectAssigneeContains(Lead_.ASIGNEE_ID, assignee));

		if (createdStartDate != null && createdStartDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateGreaterThan(Lead_.CREATED, createdStartDate));

		if (createdEndDate != null && createdEndDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateLessThan(Lead_.CREATED, createdEndDate));

		if (leadStatus != null && leadStatus.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereEnumFieldEquals(Lead_.STATUS, leadStatus, LeadStatusEnum.class));

		if (stagnantStatus != null && stagnantStatus.size() > 0)
		{
			Specification<Lead> internalSpec = null;
			for (String str : stagnantStatus)
			{
				if (str.equalsIgnoreCase("NoColour"))
					internalSpec = specbldr.specOrCondition(internalSpec,
							specbldr.whereDirectFieldIntBetween(Lead_.STAGNANT_DAYS_COUNT, 0, 10));
				if (str.equalsIgnoreCase("Green"))
					internalSpec = specbldr.specOrCondition(internalSpec,
							specbldr.whereDirectFieldIntBetween(Lead_.STAGNANT_DAYS_COUNT, 11, 20));
				if (str.equalsIgnoreCase("Orange"))
					internalSpec = specbldr.specOrCondition(internalSpec,
							specbldr.whereDirectFieldIntBetween(Lead_.STAGNANT_DAYS_COUNT, 21, 30));
				if (str.equalsIgnoreCase("Red"))
					internalSpec = specbldr.specOrCondition(internalSpec,
							specbldr.whereDirectFieldIntBetween(Lead_.STAGNANT_DAYS_COUNT, 31, Integer.MAX_VALUE));
			}
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}
		return finalSpec;
	}

}
