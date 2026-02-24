package com.ec.application.Filters;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentInventory_;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.model.*;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.ec.application.ReusableClasses.ReusableMethods.resolveCutoffDate;

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
        List<String> staleBuckets = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "staleBuckets");
        List<String> statusChangedTo = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedTo");
        List<String> statusChangedAfterDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedAfterDate");
        List<String> statusChangedBeforeDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedBeforeDate");
        List<String> tenants = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "tenants");
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

        if (tenants != null && !tenants.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals(IndentInventory_.TENANT, tenants));

        if (lineItemStatus != null && !lineItemStatus.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentContainsLineItemStatus(lineItemStatus, IndentInventory_.INVENTORY_LIST));

        if (categoryNames != null && !categoryNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereIndentCategoryContains(categoryNames, IndentInventory_.INVENTORY_LIST));

        if (staleBuckets != null && !staleBuckets.isEmpty()) {
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereIndentStatusNotIn(IndentStatusConstants.getTerminalStatuses()));
            String staleBucketKey = staleBuckets.get(0); // single-select UI
            Date cutoffDate = resolveCutoffDate(staleBucketKey);
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereIndentLastStatusUpdatedBefore(cutoffDate));
        }

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

        // ================= STATUS HISTORY FILTER =================

        if ((statusChangedTo != null && !statusChangedTo.isEmpty())
                || (statusChangedAfterDate != null && !statusChangedAfterDate.isEmpty())
                || (statusChangedBeforeDate != null && !statusChangedBeforeDate.isEmpty())) {

            finalSpec = specbldr.specAndCondition(finalSpec,
                    whereIndentHistoryStatusChangedBetween(
                            statusChangedTo,
                            statusChangedAfterDate,
                            statusChangedBeforeDate
                    ));
        }
        return finalSpec;
    }

    public static Specification<IndentInventory> getTenantSpecification(List<String> tenantNames, Specification<IndentInventory> spec) {
        Specification<IndentInventory> tenantSpec = specbldr.whereDirectFieldEquals(IndentInventory_.TENANT, tenantNames);
        return specbldr.specAndCondition(spec, tenantSpec);
    }

    private static Specification<IndentInventory> whereIndentHistoryStatusChangedBetween(List<String> statusChangedTo, List<String> statusChangedAfterDate, List<String> statusChangedBeforeDate) {
        return (root, query, cb) -> {
            query.distinct(true);   // VERY IMPORTANT (one indent → many history rows)
            Join<IndentInventory, IndentStatusHistory> historyJoin = root.join("statusHistory", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            // Filter by newStatus
            if (statusChangedTo != null && !statusChangedTo.isEmpty()) {
                predicates.add(historyJoin.get("newStatus").in(statusChangedTo));
            }

            // Date format matches your @JsonFormat
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            try {
                if (statusChangedAfterDate != null && !statusChangedAfterDate.isEmpty()) {
                    Date afterDate = sdf.parse(statusChangedAfterDate.get(0));
                    predicates.add(cb.greaterThanOrEqualTo(historyJoin.get("changedAt"), ReusableMethods.atStartOfDay(afterDate)));
                }

                if (statusChangedBeforeDate != null && !statusChangedBeforeDate.isEmpty()) {
                    Date beforeDate = sdf.parse(statusChangedBeforeDate.get(0));
                    predicates.add(cb.lessThanOrEqualTo(historyJoin.get("changedAt"), ReusableMethods.atEndOfDay(beforeDate)));
                }

            } catch (ParseException e) {
                throw new RuntimeException("Invalid date format in status history filter", e);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}
