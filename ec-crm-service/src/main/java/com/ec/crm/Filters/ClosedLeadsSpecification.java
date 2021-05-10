package com.ec.crm.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Model.Broker_;
import com.ec.crm.Model.ClosedLeads;
import com.ec.crm.Model.ClosedLeads_;
import com.ec.crm.Model.Lead_;
import com.ec.crm.Model.Source_;
import com.ec.crm.ReusableClasses.ReusableMethods;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

public final class ClosedLeadsSpecification
{
	static SpecificationsBuilder<ClosedLeads> specbldr = new SpecificationsBuilder<ClosedLeads>();

	public static Specification<ClosedLeads> getSpecification(FilterDataList filterDataList) throws Exception
	{
		List<String> name = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
		List<String> mobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "mobile");
		List<String> broker = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "broker");
		List<String> source = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "source");
		List<String> propertytype = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "propertyType");
		List<String> assignee = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "assignee");
		List<String> createdBy = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "createdBy");
		List<String> createdStartDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"createdStartDate");
		List<String> createdEndDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "createdEndDate");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
		Specification<ClosedLeads> finalSpec = null;

		if (name != null && name.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(ClosedLeads_.customerName.getName(), name));

		if (mobile != null && mobile.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(Lead_.PRIMARY_MOBILE, mobile)
							.or(specbldr.whereDirectFieldContains(Lead_.SECONDARY_MOBILE, mobile)));

		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<ClosedLeads> globalSearchSpec = specbldr
					.whereDirectFieldContains(ClosedLeads_.CUSTOMER_NAME, globalSearch)
					.or(specbldr.whereDirectFieldContains(ClosedLeads_.PRIMARY_MOBILE, globalSearch));

			finalSpec = specbldr.specAndCondition(finalSpec, globalSearchSpec);

		}

		if (broker != null && broker.size() > 0)
		{
			if (ReusableMethods.isNumeric(broker.get(0)))
				finalSpec = specbldr.specAndCondition(finalSpec,
						specbldr.whereChildLongFieldContains(ClosedLeads_.BROKER, Broker_.BROKER_ID, broker));
			else
				finalSpec = specbldr.specAndCondition(finalSpec,
						specbldr.whereChildFieldContains(ClosedLeads_.BROKER, Broker_.BROKER_NAME, broker));
		}

		if (source != null && source.size() > 0)
		{
			if (ReusableMethods.isNumeric(source.get(0)))
				finalSpec = specbldr.specAndCondition(finalSpec,
						specbldr.whereChildLongFieldContains(ClosedLeads_.SOURCE, Source_.SOURCE_ID, source));
			else
				finalSpec = specbldr.specAndCondition(finalSpec,
						specbldr.whereChildFieldContains(ClosedLeads_.SOURCE, Source_.SOURCE_NAME, source));
		}
		if (propertytype != null && propertytype.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereEnumFieldEquals(ClosedLeads_.PROPERTY_TYPE, propertytype, PropertyTypeEnum.class));

		if (createdBy != null && createdBy.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectAssigneeContains(ClosedLeads_.CREATOR_ID, createdBy));

		if (assignee != null && assignee.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectAssigneeContains(ClosedLeads_.ASIGNEE_ID, assignee));

		if (createdStartDate != null && createdStartDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateGreaterThanOrEqual(ClosedLeads_.CREATED, createdStartDate));

		if (createdEndDate != null && createdEndDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateLessThanOrEqual(ClosedLeads_.CREATED, createdEndDate));

		return finalSpec;
	}

}
