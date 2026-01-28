package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory_;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.model.*;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IndentInventorySpecification {

    static SpecificationsBuilder<IndentInventory> specbldr = new SpecificationsBuilder<IndentInventory>();

    public static Specification<IndentInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> statusList = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "indentStatus");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> lineItemStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "lineItemStatus");
        Specification<IndentInventory> finalSpec = null;

        if (startDates != null && !startDates.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan(IndentInventory_.INDENT_DATE, startDates));

        if (endDates != null && !endDates.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan(IndentInventory_.INDENT_DATE, endDates));

        if (productNames != null && !productNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsProductName(productNames, IndentInventory_.INVENTORY_LIST));

        if (productCodes != null && !productCodes.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsProductCode(productCodes, IndentInventory_.INVENTORY_LIST));

        if (statusList != null && !statusList.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals(IndentInventory_.INDENT_STATUS, statusList));

        if (lineItemStatus != null && !lineItemStatus.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsLineItemStatus(lineItemStatus, IndentInventory_.INVENTORY_LIST));

        if (categoryNames != null && !categoryNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentCategoryContains(categoryNames, IndentInventory_.INVENTORY_LIST));

        if (globalSearch != null && !globalSearch.isEmpty()) {
            Specification<IndentInventory> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_ID, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.TENANT, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereIndentContainsProductName(globalSearch, IndentInventory_.INVENTORY_LIST));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereIndentContainsProductCode(globalSearch, IndentInventory_.INVENTORY_LIST));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereIndentCategoryContains(globalSearch, IndentInventory_.INVENTORY_LIST));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }

    public static Specification<IndentInventory> getTenantSpecification(String tenantName, Specification<IndentInventory> spec) {
        Specification<IndentInventory> tenantSpec = specbldr.whereDirectFieldEquals(IndentInventory_.TENANT, Collections.singletonList(tenantName));
        return specbldr.specAndCondition(spec, tenantSpec);
    }
}
