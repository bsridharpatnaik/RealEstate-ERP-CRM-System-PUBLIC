package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class OutwardInventorySpecification {

    private static final SpecificationsBuilder<OutwardInventory> specbldr = new SpecificationsBuilder<>();

    public static Specification<OutwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> categoryNames   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> contractorNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "contractorNames");
        List<String> warehouseNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouseNames");
        List<String> usageLocations  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageLocation");
        List<String> usageAreas      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageArea");
        List<String> globalSearch    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> showOnlyReturned = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "showOnlyReturned");
        List<String> showOnlyRejected = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "showOnlyRejected");
        List<String> boqBypassed     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "boqBypassed");
        List<String> fifoOverride    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "fifoOverride");
        List<String> textSearch      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "textSearch");

        Specification<OutwardInventory> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(OutwardInventory_.DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(OutwardInventory_.DATE, endDates));

        // Dedicated filters — exact match via collection JOIN (query.distinct applied inside builder)
        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereProductContains(productNames, OutwardInventory_.INWARD_OUTWARD_LIST));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereCategoryContains(categoryNames, OutwardInventory_.INWARD_OUTWARD_LIST));

        if (notEmpty(contractorNames))
            spec = and(spec, specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, contractorNames));

        if (notEmpty(warehouseNames))
            spec = and(spec, specbldr.whereChildFieldContains(OutwardInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));

        if (notEmpty(usageLocations))
            spec = and(spec, specbldr.whereChildFieldEquals(OutwardInventory_.USAGE_LOCATION, UsageLocation_.LOCATION_NAME, usageLocations));

        if (notEmpty(usageAreas))
            spec = and(spec, specbldr.whereChildFieldEquals(OutwardInventory_.USAGE_AREA, UsageArea_.USAGE_AREA_NAME, usageAreas));

        if (notEmpty(globalSearch)) {
            Specification<OutwardInventory> gs = null;
            gs = or(gs, specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, globalSearch));
            gs = or(gs, lineItemProductNameLike(globalSearch));
            spec = and(spec, gs);
        }

        if (notEmpty(showOnlyRejected) && Boolean.parseBoolean(showOnlyRejected.get(0)))
            spec = and(spec, (root, query, cb) ->
                    cb.greaterThan(cb.size(root.get(OutwardInventory_.REJECT_OUTWARD_LIST)), 0));

        if (notEmpty(showOnlyReturned) && Boolean.parseBoolean(showOnlyReturned.get(0)))
            spec = and(spec, (root, query, cb) ->
                    cb.greaterThan(cb.size(root.get(OutwardInventory_.RETURN_OUTWARD_LIST)), 0));

        if (notEmpty(boqBypassed)) {
            boolean bypassed = Boolean.parseBoolean(boqBypassed.get(0));
            spec = and(spec, bypassed
                    ? (root, query, cb) -> cb.isNull(root.get(OutwardInventory_.HAS_BO_Q))
                    : (root, query, cb) -> cb.isNotNull(root.get(OutwardInventory_.HAS_BO_Q)));
        }

        if (notEmpty(fifoOverride) && Boolean.parseBoolean(fifoOverride.get(0)))
            spec = and(spec, (root, query, cb) -> cb.isTrue(root.get(OutwardInventory_.HAS_FIFO_OVERRIDE)));

        if (notEmpty(textSearch)) {
            Specification<OutwardInventory> ts = null;
            ts = or(ts, specbldr.whereDirectFieldContains(OutwardInventory_.SLIP_NO, textSearch));
            ts = or(ts, specbldr.whereDirectFieldContains(OutwardInventory_.ADDITIONAL_INFO, textSearch));
            ts = or(ts, specbldr.whereDirectFieldContains(OutwardInventory_.PURPOSE, textSearch));
            spec = and(spec, ts);
        }

        return spec;
    }

    // ── Subquery helpers ─────────────────────────────────────────────────────

    /** globalSearch: LIKE on product name/code via parent-ID IN subquery. */
    private static Specification<OutwardInventory> lineItemProductNameLike(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<OutwardInventory> parent = sub.from(OutwardInventory.class);
            Join<OutwardInventory, InwardOutwardList> items =
                    parent.join(OutwardInventory_.INWARD_OUTWARD_LIST, JoinType.INNER);
            Join<InwardOutwardList, Product> product = items.join(InwardOutwardList_.PRODUCT, JoinType.INNER);
            sub.select(parent.get(OutwardInventory_.OUTWARDID));
            List<Predicate> orPreds = new ArrayList<>();
            for (String term : terms) {
                String like = "%" + term + "%";
                orPreds.add(cb.like(product.get(Product_.PRODUCT_NAME), like));
                orPreds.add(cb.like(product.get(Product_.PRODUCT_CODE), like));
            }
            sub.where(cb.or(orPreds.toArray(new Predicate[0])));
            return root.get(OutwardInventory_.OUTWARDID).in(sub);
        };
    }

    // ── Composition helpers ──────────────────────────────────────────────────

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static <T> Specification<T> or(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.or(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
