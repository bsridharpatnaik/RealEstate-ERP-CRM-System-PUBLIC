package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.InventoryTransfer;
import com.ec.application.model.InventoryTransfer_;
import com.ec.application.model.InventoryTransfer;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class InventoryTransferSpecification {
    static SpecificationsBuilder<InventoryTransfer> specbldr = new SpecificationsBuilder<InventoryTransfer>();

    public static Specification<InventoryTransfer> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> sourceTenant = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "sourceTenant");
        List<String> targetTenant = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "targetTenant");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");


        Specification<InventoryTransfer> finalSpec = null;
/*
        if (startDates != null && startDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan(InventoryTransfer_.INDENT_DATE, startDates));

        if (endDates != null && endDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan(InventoryTransfer_.INDENT_DATE, endDates));

        if (productNames != null && productNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsProduct(productNames, InventoryTransfer_.INVENTORY_LIST));

        if (statusList != null && statusList.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(InventoryTransfer_.INDENT_STATUS, statusList));

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<InventoryTransfer> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(InventoryTransfer_.INDENT_STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(InventoryTransfer_.INDENT_ID, globalSearch));
            //TODO - add more filters as needed
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }*/
        return finalSpec;
    }
}
