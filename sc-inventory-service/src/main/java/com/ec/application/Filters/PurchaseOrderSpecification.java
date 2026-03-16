package com.ec.application.Filters;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.ec.application.ReusableClasses.ReusableMethods.resolveCutoffDate;

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
        List<String> staleBuckets = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "staleBuckets");
        List<String> statusChangedTo = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedTo");
        List<String> statusChangedAfterDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedAfterDate");
        List<String> statusChangedBeforeDate = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedBeforeDate");
        List<String> isSpecialPo = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "isSpecialPo");

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

        if (suppliers != null && !suppliers.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldEquals(PurchaseOrder_.SUPPLIER, Supplier_.NAME, suppliers));

        if (categoryNames != null && !categoryNames.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.wherePurchaseOrderCategoryContains(categoryNames, PurchaseOrder_.LINES));

        if (staleBuckets != null && !staleBuckets.isEmpty()) {
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.wherePOStatusNotIn(POStatusConstants.getTerminalStatuses()));
            String staleBucketKey = staleBuckets.get(0); // single-select UI
            Date cutoffDate = resolveCutoffDate(staleBucketKey);
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.wherePOLastStatusUpdatedBefore((cutoffDate)));
        }

        if (isSpecialPo != null && !isSpecialPo.isEmpty()) {
            boolean splFlag = Boolean.parseBoolean(isSpecialPo.get(0));
            finalSpec = specbldr.specAndCondition(finalSpec,
                    (root, query, cb) -> cb.equal(root.get("isSpecialPo"), splFlag));
        }

        if (globalSearch != null && !globalSearch.isEmpty()) {
            Specification<PurchaseOrder> internalSpec = null;
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.STATUS, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.PURCHASE_ORDER_ID, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.NOTES, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.SHORT_CLOSE_REASON, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereDirectFieldContains(PurchaseOrder_.SUBJECT, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(PurchaseOrder_.FIRM, Firm_.FIRM_NAME, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(PurchaseOrder_.SUPPLIER, Supplier_.NAME, globalSearch));
            internalSpec = specbldr.specOrCondition(internalSpec, specbldr.wherePurchanseOrderContainsProductName(globalSearch, PurchaseOrder_.LINES));
            finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
        }

        if ((statusChangedTo != null && !statusChangedTo.isEmpty())
                || (statusChangedAfterDate != null && !statusChangedAfterDate.isEmpty())
                || (statusChangedBeforeDate != null && !statusChangedBeforeDate.isEmpty())) {

            finalSpec = specbldr.specAndCondition(finalSpec,
                    wherePOStatusHistoryChangedBetween(
                            statusChangedTo,
                            statusChangedAfterDate,
                            statusChangedBeforeDate
                    ));
        }


        return finalSpec;
    }

    private static Specification<PurchaseOrder> wherePOStatusHistoryChangedBetween(List<String> statusChangedTo, List<String> statusChangedAfterDate, List<String> statusChangedBeforeDate) {
        return (root, query, cb) -> {
            query.distinct(true);   // VERY IMPORTANT
            Join<PurchaseOrder, PurchaseOrderStatusHistory> historyJoin = root.join("statusHistory", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by newStatus
            if (statusChangedTo != null && !statusChangedTo.isEmpty()) {
                predicates.add(historyJoin.get("newStatus").in(statusChangedTo));
            }

            try {

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                if (statusChangedAfterDate != null && !statusChangedAfterDate.isEmpty()) {
                    Date afterDate = sdf.parse(statusChangedAfterDate.get(0));
                    predicates.add(cb.greaterThanOrEqualTo(historyJoin.get("changedAt"), ReusableMethods.atStartOfDay(afterDate)));
                }

                if (statusChangedBeforeDate != null && !statusChangedBeforeDate.isEmpty()) {
                    Date beforeDate = sdf.parse(statusChangedBeforeDate.get(0));
                    predicates.add(cb.lessThanOrEqualTo(historyJoin.get("changedAt"), ReusableMethods.atEndOfDay(beforeDate)));
                }

            } catch (ParseException e) {
                throw new RuntimeException("Invalid date format in PO status history filter", e);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}
