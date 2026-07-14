package com.ec.application.Filters;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.POStatusConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.ec.application.ReusableClasses.ReusableMethods.resolveCutoffDate;

public final class PurchaseOrderSpecification {

    private static final SpecificationsBuilder<PurchaseOrder> specbldr = new SpecificationsBuilder<>();

    public static Specification<PurchaseOrder> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDates          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates            = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> productNames        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        List<String> productCodes        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");
        List<String> statusList          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "status");
        List<String> globalSearch        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
        List<String> categoryNames       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> suppliers           = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "suppliers");
        List<String> staleBuckets        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "staleBuckets");
        List<String> statusChangedTo     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedTo");
        List<String> statusChangedAfter  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedAfterDate");
        List<String> statusChangedBefore = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "statusChangedBeforeDate");
        List<String> isSpecialPo         = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "isSpecialPo");
        List<String> projectNames        = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "projectNames");
        List<String> hasOverdueOnly      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "hasOverdueOnly");

        Specification<PurchaseOrder> spec = null;

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(PurchaseOrder_.PO_DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(PurchaseOrder_.PO_DATE, endDates));

        if (notEmpty(productNames))
            spec = and(spec, poLineExistsWithProductNameIn(productNames));

        if (notEmpty(productCodes))
            spec = and(spec, poLineExistsWithProductCodeIn(productCodes));

        if (notEmpty(statusList))
            spec = and(spec, specbldr.whereDirectFieldEquals(PurchaseOrder_.STATUS, statusList));

        if (notEmpty(suppliers))
            spec = and(spec, specbldr.whereChildFieldEquals(PurchaseOrder_.SUPPLIER, Supplier_.NAME, suppliers));

        if (notEmpty(categoryNames))
            spec = and(spec, poLineExistsWithCategoryNameIn(categoryNames));

        if (notEmpty(staleBuckets)) {
            Date cutoff = resolveCutoffDate(staleBuckets.get(0));
            spec = and(spec, wherePOStatusNotIn(POStatusConstants.getTerminalStatuses()));
            spec = and(spec, whereLastStatusUpdatedBefore(cutoff));
        }

        if (notEmpty(isSpecialPo)) {
            boolean flag = Boolean.parseBoolean(isSpecialPo.get(0));
            spec = and(spec, (root, query, cb) -> cb.equal(root.get(PurchaseOrder_.SPECIAL_PO), flag));
        }

        if (notEmpty(projectNames)) {
            boolean includeEmpty = projectNames.contains("EMPTY");
            List<String> actualNames = projectNames.stream()
                    .filter(p -> !"EMPTY".equals(p))
                    .collect(Collectors.toList());
            spec = and(spec, (root, query, cb) -> {
                List<Predicate> orPreds = new ArrayList<>();
                if (includeEmpty) {
                    orPreds.add(cb.isNull(root.get(PurchaseOrder_.PROJECT_NAME)));
                    orPreds.add(cb.equal(root.get(PurchaseOrder_.PROJECT_NAME), ""));
                }
                if (!actualNames.isEmpty())
                    orPreds.add(root.get(PurchaseOrder_.PROJECT_NAME).in(actualNames));
                return cb.or(orPreds.toArray(new Predicate[0]));
            });
        }

        if (notEmpty(globalSearch)) {
            Specification<PurchaseOrder> gs = null;
            gs = or(gs, specbldr.whereDirectFieldContains(PurchaseOrder_.PURCHASE_ORDER_ID, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(PurchaseOrder_.STATUS, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(PurchaseOrder_.SUBJECT, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(PurchaseOrder_.NOTES, globalSearch));
            gs = or(gs, specbldr.whereDirectFieldContains(PurchaseOrder_.SHORT_CLOSE_REASON, globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains(PurchaseOrder_.FIRM, Firm_.FIRM_NAME, globalSearch));
            gs = or(gs, specbldr.whereChildFieldContains(PurchaseOrder_.SUPPLIER, Supplier_.NAME, globalSearch));
            gs = or(gs, poLineMatchesSearchTerm(globalSearch));
            spec = and(spec, gs);
        }

        if (notEmpty(statusChangedTo) || notEmpty(statusChangedAfter) || notEmpty(statusChangedBefore))
            spec = and(spec, poStatusHistoryExists(statusChangedTo, statusChangedAfter, statusChangedBefore));

        if (notEmpty(hasOverdueOnly) && "true".equalsIgnoreCase(hasOverdueOnly.get(0))) {
            spec = and(spec, wherePOStatusNotIn(POStatusConstants.getTerminalStatuses()));
            spec = and(spec, poHasOverdueLine());
        }

        return spec;
    }

    // ── EXISTS subqueries: dedicated filters (exact match) ───────────────────

    private static Specification<PurchaseOrder> poLineExistsWithProductNameIn(List<String> names) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(PurchaseOrderLine_.PURCHASE_ORDER).get(PurchaseOrder_.PURCHASE_ORDER_ID),
                         root.get(PurchaseOrder_.PURCHASE_ORDER_ID)),
                line.get(PurchaseOrderLine_.PRODUCT).get(Product_.PRODUCT_NAME).in(names)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<PurchaseOrder> poLineExistsWithProductCodeIn(List<String> codes) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(PurchaseOrderLine_.PURCHASE_ORDER).get(PurchaseOrder_.PURCHASE_ORDER_ID),
                         root.get(PurchaseOrder_.PURCHASE_ORDER_ID)),
                line.get(PurchaseOrderLine_.PRODUCT).get(Product_.PRODUCT_CODE).in(codes)
            ));
            return cb.exists(sub);
        };
    }

    private static Specification<PurchaseOrder> poLineExistsWithCategoryNameIn(List<String> categories) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
            Join<PurchaseOrderLine, Product> product = line.join(PurchaseOrderLine_.PRODUCT);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                cb.equal(line.get(PurchaseOrderLine_.PURCHASE_ORDER).get(PurchaseOrder_.PURCHASE_ORDER_ID),
                         root.get(PurchaseOrder_.PURCHASE_ORDER_ID)),
                product.get(Product_.CATEGORY).get(Category_.CATEGORY_NAME).in(categories)
            ));
            return cb.exists(sub);
        };
    }

    // ── EXISTS subquery: globalSearch — LIKE across product name, code, category ──

    private static Specification<PurchaseOrder> poLineMatchesSearchTerm(List<String> terms) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
            Join<PurchaseOrderLine, Product> product = line.join(PurchaseOrderLine_.PRODUCT);
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
                cb.equal(line.get(PurchaseOrderLine_.PURCHASE_ORDER).get(PurchaseOrder_.PURCHASE_ORDER_ID),
                         root.get(PurchaseOrder_.PURCHASE_ORDER_ID)),
                cb.or(orPreds.toArray(new Predicate[0]))
            ));
            return cb.exists(sub);
        };
    }

    // ── EXISTS subquery: status history filter ───────────────────────────────

    private static Specification<PurchaseOrder> poStatusHistoryExists(
            List<String> statusChangedTo,
            List<String> statusChangedAfter,
            List<String> statusChangedBefore) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderStatusHistory> hist = sub.from(PurchaseOrderStatusHistory.class);
            sub.select(cb.literal(1L));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(
                hist.get(PurchaseOrderStatusHistory_.PURCHASE_ORDER).get(PurchaseOrder_.PURCHASE_ORDER_ID),
                root.get(PurchaseOrder_.PURCHASE_ORDER_ID)
            ));

            if (notEmpty(statusChangedTo))
                predicates.add(hist.get(PurchaseOrderStatusHistory_.NEW_STATUS).in(statusChangedTo));

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            try {
                if (notEmpty(statusChangedAfter)) {
                    Date d = sdf.parse(statusChangedAfter.get(0));
                    predicates.add(cb.greaterThanOrEqualTo(hist.get(PurchaseOrderStatusHistory_.CHANGED_AT),
                            ReusableMethods.atStartOfDay(d)));
                }
                if (notEmpty(statusChangedBefore)) {
                    Date d = sdf.parse(statusChangedBefore.get(0));
                    predicates.add(cb.lessThanOrEqualTo(hist.get(PurchaseOrderStatusHistory_.CHANGED_AT),
                            ReusableMethods.atEndOfDay(d)));
                }
            } catch (ParseException e) {
                throw new RuntimeException("Invalid date format in PO status history filter", e);
            }

            sub.where(predicates.toArray(new Predicate[0]));
            return cb.exists(sub);
        };
    }

    // ── Stale bucket helpers ─────────────────────────────────────────────────

    private static Specification<PurchaseOrder> whereLastStatusUpdatedBefore(Date cutoffDate) {
        return (root, query, cb) ->
            cb.lessThan(root.get(PurchaseOrder_.LAST_STATUS_UPDATED_AT), cutoffDate);
    }

    private static Specification<PurchaseOrder> wherePOStatusNotIn(List<String> statuses) {
        return (root, query, cb) -> cb.not(root.get(PurchaseOrder_.STATUS).in(statuses));
    }

    // ── Overdue lines filter ─────────────────────────────────────────────────

    /**
     * POs with at least one non-completed line whose effective lead time has been exceeded.
     * Effective lead time = COALESCE(product.leadTimeDays, product.category.leadTimeDays).
     * Days elapsed = DATEDIFF(CURRENT_DATE, po.poDate).
     */
    private static Specification<PurchaseOrder> poHasOverdueLine() {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
            Join<PurchaseOrderLine, Product> product = line.join(PurchaseOrderLine_.PRODUCT);
            Join<Product, Category> category = product.join(Product_.CATEGORY, JoinType.LEFT);

            // COALESCE(product.leadTimeDays, category.leadTimeDays)
            Expression<Integer> effectiveLeadTime = cb.function(
                    "COALESCE", Integer.class,
                    product.<Integer>get("leadTimeDays"),
                    category.<Integer>get("leadTimeDays")
            );

            // DATEDIFF(CURRENT_DATE, po.poDate)
            Expression<Integer> daysSincePO = cb.function(
                    "DATEDIFF", Integer.class,
                    cb.currentDate(),
                    root.get(PurchaseOrder_.PO_DATE)
            );

            sub.select(cb.literal(1L));
            sub.where(cb.and(
                    cb.equal(line.get(PurchaseOrderLine_.PURCHASE_ORDER)
                            .get(PurchaseOrder_.PURCHASE_ORDER_ID),
                            root.get(PurchaseOrder_.PURCHASE_ORDER_ID)),
                    cb.isFalse(line.get("isDeleted")),
                    cb.isNotNull(effectiveLeadTime),
                    cb.greaterThan(daysSincePO, effectiveLeadTime)
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
