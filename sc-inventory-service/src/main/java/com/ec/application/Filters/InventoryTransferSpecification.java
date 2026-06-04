package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class InventoryTransferSpecification {

    private static final SpecificationsBuilder<InventoryTransfer> specbldr = new SpecificationsBuilder<>();

    public static Specification<InventoryTransfer> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> sourceTenant = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "sourceTenant");
        List<String> targetTenant = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "targetTenant");
        List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<InventoryTransfer> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(InventoryTransfer_.TRANSFER_DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(InventoryTransfer_.TRANSFER_DATE, endDates));

        if (notEmpty(sourceTenant))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryTransfer_.SOURCE_TENANT, sourceTenant));

        if (notEmpty(targetTenant))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryTransfer_.TARGET_TENANT, targetTenant));

        if (notEmpty(productNames))
            spec = and(spec, transferItemExistsWithProductNameIn(productNames));

        if (notEmpty(productCodes))
            spec = and(spec, transferItemExistsWithProductCodeIn(productCodes));

        if (notEmpty(globalSearch)) {
            Specification<InventoryTransfer> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(InventoryTransfer_.SOURCE_TENANT, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InventoryTransfer_.TARGET_TENANT, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InventoryTransfer_.SOURCE_WAREHOUSE_NAME, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(InventoryTransfer_.TARGET_WAREHOUSE_NAME, globalSearch));
            gs = or(gs, transferItemMatchesSearchTerm(globalSearch));
            spec = and(spec, gs);
        }

        return spec;
    }

    // ── EXISTS subqueries ────────────────────────────────────────────────────

    private static Specification<InventoryTransfer> transferItemExistsWithProductNameIn(List<String> names) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InventoryTransferItem> item = sub.from(InventoryTransferItem.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(item.get(InventoryTransferItem_.INVENTORY_TRANSFER).get(InventoryTransfer_.TRANSFER_ID),
                         root.get(InventoryTransfer_.TRANSFER_ID)),
                item.get(InventoryTransferItem_.PRODUCT_NAME).in(names)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<InventoryTransfer> transferItemExistsWithProductCodeIn(List<String> codes) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InventoryTransferItem> item = sub.from(InventoryTransferItem.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(item.get(InventoryTransferItem_.INVENTORY_TRANSFER).get(InventoryTransfer_.TRANSFER_ID),
                         root.get(InventoryTransfer_.TRANSFER_ID)),
                item.get(InventoryTransferItem_.PRODUCT_CODE).in(codes)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<InventoryTransfer> transferItemMatchesSearchTerm(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<InventoryTransferItem> item = sub.from(InventoryTransferItem.class);
            sub.select(cb.literal(1L));
            List<Predicate> orPreds = new ArrayList<>();
            for (String term : terms) {
                String like = "%" + term + "%";
                orPreds.add(cb.like(item.get(InventoryTransferItem_.PRODUCT_NAME), like));
                orPreds.add(cb.like(item.get(InventoryTransferItem_.PRODUCT_CODE), like));
            }
            sub.where(cb.and(
                cb.equal(item.get(InventoryTransferItem_.INVENTORY_TRANSFER).get(InventoryTransfer_.TRANSFER_ID),
                         root.get(InventoryTransfer_.TRANSFER_ID)),
                cb.or(orPreds.toArray(new Predicate[0]))
            ));
            return cb.exists(sub);
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
