package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.model.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.util.List;

public final class IndentInventorySpecification {
    static SpecificationsBuilder<IndentInventory> specbldr = new SpecificationsBuilder<IndentInventory>();

    public static Specification<IndentInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> statusList = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "status");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        Specification<IndentInventory> finalSpec = null;

        if (startDates != null && startDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan(IndentInventory_.INDENT_DATE, startDates));

        if (endDates != null && endDates.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan(IndentInventory_.INDENT_DATE, endDates));

        if (productNames != null && productNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsProduct(productNames, IndentInventory_.INVENTORY_LIST));

        if (categoryNames != null && categoryNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentCategoryContains(categoryNames, IndentInventory_.INVENTORY_LIST));

        if (statusList != null && statusList.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, statusList));

        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<IndentInventory> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_ID, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereIndentContainsProduct(globalSearch, IndentInventory_.INVENTORY_LIST));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereIndentCategoryContains(globalSearch, IndentInventory_.INVENTORY_LIST));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(IndentInventory_.INVENTORY_LIST, IndentInventoryList_.SPECIFICATION, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(IndentInventory_.INVENTORY_LIST, IndentInventoryList_.REMARKS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(IndentInventory_.INVENTORY_LIST, IndentInventoryList_.LINE_ITEM_STATUS, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }
}
