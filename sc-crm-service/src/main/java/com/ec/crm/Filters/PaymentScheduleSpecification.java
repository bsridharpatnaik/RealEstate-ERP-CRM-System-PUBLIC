package com.ec.crm.Filters;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Model.DealStructure_;
import com.ec.crm.Model.Lead_;
import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Model.PaymentSchedule_;
import com.ec.crm.Model.PropertyName_;
import com.ec.crm.Model.PropertyType_;
import com.ec.crm.ReusableClasses.SpecificationsBuilder;

public class PaymentScheduleSpecification
{

	static SpecificationsBuilder<PaymentSchedule> specbldr = new SpecificationsBuilder<PaymentSchedule>();

	public static Specification<PaymentSchedule> getSpecification(FilterDataList filterDataList) throws Exception
	{
		Specification<PaymentSchedule> finalSpec = null;

		List<String> startDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
		List<String> endDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
		List<String> isReceived = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "isReceived");
		List<String> propertyType = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "propertyType");
		List<String> propertyName = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "propertyName");
		List<String> assignee = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "assignee");
		List<String> IsCustomerPayment = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "IsCustomerPayment");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

		if (startDate != null && startDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateGreaterThanOrEqual(PaymentSchedule_.PAYMENT_DATE, startDate));

		if (endDate != null && endDate.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateLessThanOrEqual(PaymentSchedule_.PAYMENT_DATE, endDate));

		if (isReceived != null && isReceived.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectBoleanFieldEquals(PaymentSchedule_.IS_RECEIVED, isReceived));

		if (IsCustomerPayment != null && IsCustomerPayment.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectBoleanFieldEquals(PaymentSchedule_.IS_CUSTOMER_PAYMENT, IsCustomerPayment));

		if (propertyType != null && propertyType.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldEquals(PaymentSchedule_.DS,
					DealStructure_.PROPERTY_TYPE, PropertyType_.PROPERTY_TYPE, propertyType));

		if (propertyName != null && propertyName.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldEquals(PaymentSchedule_.DS,
					DealStructure_.PROPERTY_NAME, PropertyName_.NAME, propertyName));

		if (assignee != null && assignee.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildAssigneeContains(
					PaymentSchedule_.DS, DealStructure_.LEAD, Lead_.ASIGNEE_ID, assignee));

		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<PaymentSchedule> internalSpec1 = specbldr.specAndCondition(finalSpec,
					specbldr.whereGrandChildFieldContains(PaymentSchedule_.DS, DealStructure_.LEAD, Lead_.CUSTOMER_NAME,
							globalSearch));
			Specification<PaymentSchedule> internalSpec2 = specbldr.specAndCondition(finalSpec,
					specbldr.whereGrandChildFieldContains(PaymentSchedule_.DS, DealStructure_.LEAD,
							Lead_.PRIMARY_MOBILE, globalSearch));
			Specification<PaymentSchedule> internalSpec = specbldr.specOrCondition(internalSpec1, internalSpec2);
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}
		return finalSpec;
	}
}
