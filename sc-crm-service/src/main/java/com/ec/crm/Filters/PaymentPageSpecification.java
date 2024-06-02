package com.ec.crm.Filters;

import java.util.List;

import com.ec.crm.Model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.ReusableClasses.SpecificationsBuilder;

public class PaymentPageSpecification {

    static SpecificationsBuilder<PaymentsPage> specbldr = new SpecificationsBuilder<PaymentsPage>();

    public static Specification<PaymentsPage> getSpecification(FilterDataList filterDataList) throws Exception {
        Specification<PaymentsPage> finalSpec = null;

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
                    specbldr.whereDirectFieldDateGreaterThanOrEqual(PaymentsPage_.PAYMENT_DATE, startDate));

        if (endDate != null && endDate.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThanOrEqual(PaymentsPage_.PAYMENT_DATE, endDate));

        if (isReceived != null && isReceived.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectBoleanFieldEquals(PaymentsPage_.IS_RECEIVED, isReceived));

        if (IsCustomerPayment != null && IsCustomerPayment.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectBoleanFieldEquals(PaymentsPage_.IS_CUSTOMER_PAYMENT, IsCustomerPayment));

        if (propertyType != null && propertyType.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(PaymentsPage_.PROPERTY_TYPE, propertyType));

        if (propertyName != null && propertyName.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(PaymentsPage_.PROPERTY_NAME, propertyName));

        if (assignee != null && assignee.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(PaymentsPage_.ASSIGNEE_ID, assignee));

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<PaymentsPage> internalSpec1 = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(PaymentsPage_.CUSTOMER_NAME, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec1);
        }
        return finalSpec;
    }
}
