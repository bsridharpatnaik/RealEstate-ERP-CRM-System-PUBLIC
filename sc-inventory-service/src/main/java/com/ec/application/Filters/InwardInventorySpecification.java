package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class InwardInventorySpecification {

    private static final SpecificationsBuilder<InwardInventory> specbldr = new SpecificationsBuilder<>();

    public static Specification<InwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> supplierNames     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "supplierNames");
        List<String> warehouseNames    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouseNames");
        List<String> invoiceReceived   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "invoiceReceived");
        List<String> globalSearch      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> showOnlyRejected  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "showOnlyRejected");
        List<String> categoryNames     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> missingChallanBill = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "missingChallanBill");

        Specification<InwardInventory> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(InwardInventory_.DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(InwardInventory_.DATE, endDates));

        // Dedicated filters — exact match via collection JOIN (query.distinct applied inside builder)
        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereProductContains(productNames, InwardInventory_.INWARD_OUTWARD_LIST));

        if (notEmpty(productCodes))
            spec = and(spec, lineItemExistsWithProductCodeIn(productCodes));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereCategoryContains(categoryNames, InwardInventory_.INWARD_OUTWARD_LIST));

        if (notEmpty(supplierNames))
            spec = and(spec, specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, supplierNames));

        if (notEmpty(warehouseNames))
            spec = and(spec, warehouseExistsIn(warehouseNames));

        if (notEmpty(invoiceReceived))
            spec = and(spec, specbldr.whereDirectBoleanFieldEquals(InwardInventory_.INVOICE_RECEIVED, invoiceReceived));

        if (notEmpty(globalSearch)) {
            Specification<InwardInventory> gs = null;
            gs = or(gs, inwardIdMatch(globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, globalSearch));
            gs = or(gs, lineItemProductNameLike(globalSearch));
            gs = or(gs, warehouseNameLike(globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.BILL_NO, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.CHALLAN_NO, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.ADDITIONAL_INFO, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.PURCHASE_ORDER_NO, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.OUR_SLIP_NO, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InwardInventory_.VEHICLE_NO, globalSearch));
            spec = and(spec, gs);
        }

        if (notEmpty(showOnlyRejected) && Boolean.parseBoolean(showOnlyRejected.get(0)))
            spec = and(spec, (root, query, cb) ->
                    cb.greaterThan(cb.size(root.get(InwardInventory_.REJECT_INWARD_LIST)), 0));

        if (notEmpty(missingChallanBill) && Boolean.parseBoolean(missingChallanBill.get(0)))
            spec = and(spec, (root, query, cb) -> cb.and(
                    cb.or(cb.isNull(root.get(InwardInventory_.CHALLAN_NO)),
                          cb.equal(cb.trim(root.get(InwardInventory_.CHALLAN_NO)), "")),
                    cb.or(cb.isNull(root.get(InwardInventory_.BILL_NO)),
                          cb.equal(cb.trim(root.get(InwardInventory_.BILL_NO)), ""))
            ));

        return spec;
    }

    // ── Subquery helpers ─────────────────────────────────────────────────────

    /** Exact-match product code filter via parent-ID IN subquery (avoids DISTINCT fan-out). */
    private static Specification<InwardInventory> lineItemExistsWithProductCodeIn(List<String> codes) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InwardInventory> parent = sub.from(InwardInventory.class);
            Join<InwardInventory, InwardOutwardList> items =
                    parent.join(InwardInventory_.INWARD_OUTWARD_LIST, JoinType.INNER);
            Join<InwardOutwardList, Product> product = items.join(InwardOutwardList_.PRODUCT, JoinType.INNER);
            sub.select(parent.get(InwardInventory_.INWARD_ID));
            sub.where(product.get(Product_.PRODUCT_CODE).in(codes));
            return root.get(InwardInventory_.INWARD_ID).in(sub);
        };
    }

    /** globalSearch: exact match on inwardId if the search term is a valid number. */
    private static Specification<InwardInventory> inwardIdMatch(List<String> terms) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            for (String term : terms) {
                try {
                    Long id = Long.parseLong(term.trim());
                    preds.add(cb.equal(root.get(InwardInventory_.INWARD_ID), id));
                } catch (NumberFormatException ignored) {}
            }
            if (preds.isEmpty()) return cb.disjunction();
            return cb.or(preds.toArray(new Predicate[0]));
        };
    }

    /** globalSearch: LIKE on product name via parent-ID IN subquery. */
    private static Specification<InwardInventory> lineItemProductNameLike(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InwardInventory> parent = sub.from(InwardInventory.class);
            Join<InwardInventory, InwardOutwardList> items =
                    parent.join(InwardInventory_.INWARD_OUTWARD_LIST, JoinType.INNER);
            Join<InwardOutwardList, Product> product = items.join(InwardOutwardList_.PRODUCT, JoinType.INNER);
            sub.select(parent.get(InwardInventory_.INWARD_ID));
            List<Predicate> orPreds = new ArrayList<>();
            for (String term : terms) {
                String like = "%" + term + "%";
                orPreds.add(cb.like(product.get(Product_.PRODUCT_NAME), like));
                orPreds.add(cb.like(product.get(Product_.PRODUCT_CODE), like));
            }
            sub.where(cb.or(orPreds.toArray(new Predicate[0])));
            return root.get(InwardInventory_.INWARD_ID).in(sub);
        };
    }

    /** globalSearch: LIKE on warehouse name via line-item subquery. */
    private static Specification<InwardInventory> warehouseNameLike(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InwardInventory> parent = sub.from(InwardInventory.class);
            Join<InwardInventory, InwardOutwardList> items =
                    parent.join(InwardInventory_.INWARD_OUTWARD_LIST, JoinType.INNER);
            Join<InwardOutwardList, Warehouse> warehouse = items.join(InwardOutwardList_.WAREHOUSE, JoinType.INNER);
            sub.select(parent.get(InwardInventory_.INWARD_ID));
            List<Predicate> orPreds = new ArrayList<>();
            for (String term : terms) {
                orPreds.add(cb.like(cb.lower(warehouse.get(Warehouse_.WAREHOUSE_NAME)), "%" + term.toLowerCase() + "%"));
            }
            sub.where(cb.or(orPreds.toArray(new Predicate[0])));
            return root.get(InwardInventory_.INWARD_ID).in(sub);
        };
    }

    /** Warehouse filter: case-insensitive via parent-ID IN subquery. */
    private static Specification<InwardInventory> warehouseExistsIn(List<String> warehouseNames) {
        return (root, query, cb) -> {
            List<String> lower = warehouseNames.stream().map(String::toLowerCase).collect(Collectors.toList());
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InwardInventory> parent = sub.from(InwardInventory.class);
            Join<InwardInventory, InwardOutwardList> items =
                    parent.join(InwardInventory_.INWARD_OUTWARD_LIST, JoinType.INNER);
            Join<InwardOutwardList, Warehouse> warehouse = items.join(InwardOutwardList_.WAREHOUSE, JoinType.INNER);
            sub.select(parent.get(InwardInventory_.INWARD_ID));
            sub.where(cb.lower(warehouse.get(Warehouse_.WAREHOUSE_NAME)).in(lower));
            return root.get(InwardInventory_.INWARD_ID).in(sub);
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
