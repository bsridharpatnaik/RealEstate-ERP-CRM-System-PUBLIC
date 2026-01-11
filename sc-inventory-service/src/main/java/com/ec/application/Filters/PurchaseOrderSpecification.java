package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory_;
import com.ec.application.model.PurchaseOrder;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class PurchaseOrderSpecification {

    static SpecificationsBuilder<PurchaseOrder> specbldr = new SpecificationsBuilder<PurchaseOrder>();

    public static Specification<PurchaseOrder> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> statusList = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "status");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        Specification<PurchaseOrder> finalSpec = null;
/*
        if (startDates != null && startDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan(IndentInventory_.INDENT_DATE, startDates));

        if (endDates != null && endDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan(IndentInventory_.INDENT_DATE, endDates));

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<IndentInventory> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_ID, globalSearch));
            //TODO - add more filters as needed
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }*/
        return finalSpec;
    }
}
