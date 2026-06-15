package com.ec.application.Filters;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.IndentStatusConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ec.application.ReusableClasses.ReusableMethods.resolveCutoffDate;

public final class IndentInventorySpecification {

    private static final SpecificationsBuilder<IndentInventory> specbldr = new SpecificationsBuilder<>();

    public static Specification<IndentInventory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates            = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> statusList          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "indentStatus");
        List<String> globalSearch        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> categoryNames       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> lineItemStatuses    = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "lineItemStatus");
        List<String> staleBuckets        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "staleBuckets");
        List<String> statusChangedTo     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedTo");
        List<String> statusChangedAfter  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedAfterDate");
        List<String> statusChangedBefore = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedBeforeDate");
        List<String> tenants             = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "tenants");

        Specification<IndentInventory> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(IndentInventory_.INDENT_DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(IndentInventory_.INDENT_DATE, endDates));

        if (notEmpty(productNames))
            spec = and(spec, lineItemExistsWithProductNameIn(productNames));

        if (notEmpty(productCodes))
            spec = and(spec, lineItemExistsWithProductCodeIn(productCodes));

        if (notEmpty(statusList))
            spec = and(spec, specbldr.whereDirectFieldEquals(IndentInventory_.INDENT_STATUS, statusList));

        if (notEmpty(tenants))
            spec = and(spec, specbldr.whereDirectFieldEquals(IndentInventory_.TENANT, tenants));

        if (notEmpty(lineItemStatuses))
            spec = and(spec, lineItemExistsWithStatusIn(lineItemStatuses));

        if (notEmpty(categoryNames))
            spec = and(spec, lineItemExistsWithCategoryNameIn(categoryNames));

        if (notEmpty(staleBuckets)) {
            Date cutoff = resolveCutoffDate(staleBuckets.get(0));
            spec = and(spec, whereIndentStatusNotIn(IndentStatusConstants.getTerminalStatuses()));
            spec = and(spec, whereLastStatusUpdatedBefore(cutoff));
        }

        if (notEmpty(globalSearch)) {
            Specification<IndentInventory> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_ID, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(IndentInventory_.INDENT_STATUS, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(IndentInventory_.TENANT, globalSearch));
            gs = or(gs, lineItemMatchesSearchTerm(globalSearch));
            spec = and(spec, gs);
        }

        if (notEmpty(statusChangedTo) || notEmpty(statusChangedAfter) || notEmpty(statusChangedBefore))
            spec = and(spec, statusHistoryExists(statusChangedTo, statusChangedAfter, statusChangedBefore));

        return spec;
    }

    public static Specification<IndentInventory> buildSpecificationWithTenantSecurity(
            FilterDataList filterDataList,
            String headerTenant,
            List<String> allowedTenants) throws ParseException {

        Specification<IndentInventory> spec = getSpecification(filterDataList);

        if (headerTenant != null)
            spec = and(spec, specbldr.whereDirectFieldEquals(IndentInventory_.TENANT,
                    Collections.singletonList(headerTenant)));
        else if (notEmpty(allowedTenants))
            spec = and(spec, specbldr.whereDirectFieldEquals(IndentInventory_.TENANT, allowedTenants));

        return spec == null ? Specification.where(null) : spec;
    }

    // ── EXISTS subqueries: dedicated filters (exact match) ───────────────────

    private static Specification<IndentInventory> lineItemExistsWithProductNameIn(List<String> names) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentInventoryList> line = sub.from(IndentInventoryList.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(IndentInventoryList_.INDENT_INVENTORY).get(IndentInventory_.INDENT_ID),
                         root.get(IndentInventory_.INDENT_ID)),
                line.get(IndentInventoryList_.PRODUCT).get(Product_.PRODUCT_NAME).in(names)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<IndentInventory> lineItemExistsWithProductCodeIn(List<String> codes) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentInventoryList> line = sub.from(IndentInventoryList.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(IndentInventoryList_.INDENT_INVENTORY).get(IndentInventory_.INDENT_ID),
                         root.get(IndentInventory_.INDENT_ID)),
                line.get(IndentInventoryList_.PRODUCT).get(Product_.PRODUCT_CODE).in(codes)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<IndentInventory> lineItemExistsWithStatusIn(List<String> statuses) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentInventoryList> line = sub.from(IndentInventoryList.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(IndentInventoryList_.INDENT_INVENTORY).get(IndentInventory_.INDENT_ID),
                         root.get(IndentInventory_.INDENT_ID)),
                line.get(IndentInventoryList_.LINE_ITEM_STATUS).in(statuses)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<IndentInventory> lineItemExistsWithCategoryNameIn(List<String> categories) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentInventoryList> line = sub.from(IndentInventoryList.class);
            Join<IndentInventoryList, Product> product = line.join(IndentInventoryList_.PRODUCT);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(IndentInventoryList_.INDENT_INVENTORY).get(IndentInventory_.INDENT_ID),
                         root.get(IndentInventory_.INDENT_ID)),
                product.get(Product_.CATEGORY).get(Category_.CATEGORY_NAME).in(categories)
            ));
            return cb.exists(sub);
        };
    }

    // ── EXISTS subquery: globalSearch — LIKE across product name, code, category ──

    private static Specification<IndentInventory> lineItemMatchesSearchTerm(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentInventoryList> line = sub.from(IndentInventoryList.class);
            Join<IndentInventoryList, Product> product = line.join(IndentInventoryList_.PRODUCT);
            Join<Product, Category> category = product.join(Product_.CATEGORY, JoinType.LEFT);
            sub.select(cb.literal(1L));

            List<Predicate> orPreds = new ArrayList<>();
            for (String term : terms) {
                String like = "%" + term + "%";
                orPreds.add(cb.like(product.get(Product_.PRODUCT_NAME), like));
                orPreds.add(cb.like(product.get(Product_.PRODUCT_CODE), like));
                orPreds.add(cb.like(category.get(Category_.CATEGORY_NAME), like));
            }

            sub.where(cb.and(
                cb.equal(line.get(IndentInventoryList_.INDENT_INVENTORY).get(IndentInventory_.INDENT_ID),
                         root.get(IndentInventory_.INDENT_ID)),
                cb.or(orPreds.toArray(new Predicate[0]))
            ));
            return cb.exists(sub);
        };
    }

    // ── EXISTS subquery: status history filter ───────────────────────────────

    private static Specification<IndentInventory> statusHistoryExists(
            List<String> statusChangedTo,
            List<String> statusChangedAfter,
            List<String> statusChangedBefore) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<IndentStatusHistory> hist = sub.from(IndentStatusHistory.class);
            sub.select(cb.literal(1L));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(
                hist.get(IndentStatusHistory_.INDENT).get(IndentInventory_.INDENT_ID),
                root.get(IndentInventory_.INDENT_ID)
            ));

            if (notEmpty(statusChangedTo))
                predicates.add(hist.get(IndentStatusHistory_.NEW_STATUS).in(statusChangedTo));

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            try {
                if (notEmpty(statusChangedAfter)) {
                    Date d = sdf.parse(statusChangedAfter.get(0));
                    predicates.add(cb.greaterThanOrEqualTo(hist.get(IndentStatusHistory_.CHANGED_AT),
                            ReusableMethods.atStartOfDay(d)));
                }
                if (notEmpty(statusChangedBefore)) {
                    Date d = sdf.parse(statusChangedBefore.get(0));
                    predicates.add(cb.lessThanOrEqualTo(hist.get(IndentStatusHistory_.CHANGED_AT),
                            ReusableMethods.atEndOfDay(d)));
                }
            } catch (ParseException e) {
                throw new RuntimeException("Invalid date format in status history filter", e);
            }

            sub.where(predicates.toArray(new Predicate[0]));
            return cb.exists(sub);
        };
    }

    // ── Stale bucket helpers ─────────────────────────────────────────────────

    private static Specification<IndentInventory> whereLastStatusUpdatedBefore(Date cutoffDate) {
        return (root, query, cb) ->
            cb.lessThan(root.get(IndentInventory_.LAST_STATUS_UPDATED_AT), cutoffDate);
    }

    private static Specification<IndentInventory> whereIndentStatusNotIn(List<String> statuses) {
        return (root, query, cb) -> cb.not(root.get(IndentInventory_.INDENT_STATUS).in(statuses));
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
