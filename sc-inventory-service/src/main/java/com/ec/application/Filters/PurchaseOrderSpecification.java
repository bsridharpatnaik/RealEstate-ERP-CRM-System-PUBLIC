package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
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
        List<String> suppliers = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "suppliers");
        Specification<PurchaseOrder> finalSpec = null;

        if (startDates != null && !startDates.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan(PurchaseOrder_.PO_DATE, startDates));

        if (endDates != null && !endDates.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan(PurchaseOrder_.PO_DATE, endDates));

        if (productNames != null && !productNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.wherePurchanseOrderContainsProductName(productNames, PurchaseOrder_.LINES));

        if (productCodes != null && !productCodes.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.wherePurchanseOrderContainsProductCode(productCodes, PurchaseOrder_.LINES));

        if (statusList != null && !statusList.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals(PurchaseOrder_.STATUS, statusList));

        if (categoryNames != null && !categoryNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.wherePurchaseOrderCategoryContains(categoryNames, PurchaseOrder_.LINES));

        if (globalSearch != null && !globalSearch.isEmpty()) {
            Specification<PurchaseOrder> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.PURCHASE_ORDER_ID, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.NOTES, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.SHORT_CLOSE_REASON, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.SUBJECT, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(PurchaseOrder_.FIRM, Firm_.FIRM_NAME, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(PurchaseOrder_.SUPPLIER, Supplier_.NAME, globalSearch));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }
        return finalSpec;
    }
}
