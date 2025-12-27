package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory_;
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
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
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
//TODO - add more filters as needed
        /*
        if (categoryNames != null && categoryNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentCategoryContains(categoryNames, IndentInventory_.INVENTORY_LIST));

        if (statusList != null && statusList.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, statusList));
*/
        if (globalSearch != null && globalSearch.size() > 0) {
            Specification<IndentInventory> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_ID, globalSearch));
            //TODO - add more filters as needed
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }
}
