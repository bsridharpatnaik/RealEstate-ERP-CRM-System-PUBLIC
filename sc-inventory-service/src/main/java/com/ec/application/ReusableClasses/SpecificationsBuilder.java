package com.ec.application.ReusableClasses;

import javax.persistence.criteria.*;

import com.ec.application.constants.ProjectConstants;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.Filters.BOQStatusFilterDataList;
import com.ec.application.Filters.FilterAttributeData;
import com.ec.application.Filters.FilterDataList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Generic reusable predicates for simple field-level filtering.
 * Complex multi-join or EXISTS-based filters belong in the individual Specification classes.
 */
public class SpecificationsBuilder<T> {

    String dateFormat = ProjectConstants.dateFormat;

    // ── Direct field predicates ──────────────────────────────────────────────

    public Specification<T> whereDirectFieldContains(String key, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.like(root.get(key), "%" + name + "%"));
        }
        return spec;
    }

    /** Note: method name kept as-is for backward compatibility with existing callers. */
    public Specification<T> whereDirectBoleanFieldEquals(String key, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.equal(root.get(key), Boolean.parseBoolean(name)));
        }
        return spec;
    }

    public Specification<T> whereDirectFieldEquals(String key, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.equal(root.get(key), name));
        }
        return spec;
    }

    public Specification<T> whereDirectFieldDateGreaterThan(String key, List<String> startDates) throws ParseException {
        Date startDate = ReusableMethods.atStartOfDay(new SimpleDateFormat(dateFormat).parse(startDates.get(0)));
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(key), startDate);
    }

    public Specification<T> whereDirectFieldDateLessThan(String key, List<String> endDates) throws ParseException {
        Date endDate = ReusableMethods.atEndOfDay(new SimpleDateFormat(dateFormat).parse(endDates.get(0)));
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(key), endDate);
    }

    public Specification<T> whereDirectFieldDoubleGreaterThan(String key, Double value) {
        return (root, query, cb) -> cb.greaterThan(root.get(key), value);
    }

    public Specification<T> whereDirectFieldLongFieldContains(String key, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.equal(root.get(key), Long.parseLong(name)));
        }
        return spec;
    }

    public Specification<T> whereDirectFieldLongBetween(String key, Long lowerLimit, Long upperLimit) {
        return specAndCondition(
            (root, query, cb) -> cb.lessThanOrEqualTo(root.get(key), lowerLimit),
            (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(key), upperLimit)
        );
    }

    // ── ManyToOne path navigation (no collection join — no fan-out risk) ─────

    public Specification<T> whereChildFieldContains(String childField, String grandChildField, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.like(root.get(childField).get(grandChildField), "%" + name + "%"));
        }
        return spec;
    }

    public Specification<T> whereChildFieldEquals(String childField, String grandChildField, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.equal(root.get(childField).get(grandChildField), name));
        }
        return spec;
    }

    public Specification<T> whereGrandChildFieldContains(String childField, String grandChildField,
                                                          String ggcField, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.like(root.get(childField).get(grandChildField).get(ggcField), "%" + name + "%"));
        }
        return spec;
    }

    // ── Collection JOIN predicates (kept for Inward/Outward specs) ───────────
    // These use INNER JOIN on a collection — callers must ensure DISTINCT is applied
    // on the outer query if fan-out is possible.

    public Specification<T> whereProductContains(List<String> productNames, String joinTable) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<T, InwardOutwardList> items = root.join(joinTable);
            Join<InwardOutwardList, Product> product = items.join(InwardOutwardList_.PRODUCT);
            return product.get(Product_.PRODUCT_NAME).in(productNames);
        };
    }

    public Specification<T> whereCategoryContains(List<String> categoryNames, String joinTable) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<T, InwardOutwardList> items = root.join(joinTable);
            Join<InwardOutwardList, Product> product = items.join(InwardOutwardList_.PRODUCT);
            Join<Product, Category> category = product.join(Product_.CATEGORY);
            return category.get(Category_.CATEGORY_NAME).in(categoryNames);
        };
    }

    public Specification<T> whereWarehouseContains(List<String> warehouseNames, String joinTable) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<T, InwardOutwardList> items = root.join(joinTable);
            Join<InwardOutwardList, Warehouse> warehouse = items.join(InwardOutwardList_.WAREHOUSE);
            return warehouse.get(Warehouse_.WAREHOUSE_NAME).in(warehouseNames);
        };
    }

    public Specification<T> whereChildFieldListContains(String childTableName, String gcTable,
                                                         String fieldName, List<String> names) {
        Specification<T> spec = null;
        for (String name : names) {
            spec = specOrCondition(spec,
                (root, query, cb) -> cb.like(root.join(childTableName).join(gcTable).get(fieldName), "%" + name + "%"));
        }
        return spec;
    }

    // ── Composition helpers ──────────────────────────────────────────────────

    public Specification<T> specAndCondition(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    public Specification<T> specOrCondition(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.or(next);
    }

    // ── Filter data extraction ───────────────────────────────────────────────

    public static List<String> fetchValueFromFilterList(FilterDataList filterDataList, String field) {
        for (FilterAttributeData filterData : filterDataList.getFilterData()) {
            if (filterData.getAttrName().equalsIgnoreCase(field))
                return filterData.getAttrValue();
        }
        return null;
    }

    public static List<String> fetchValueFromBoqFilterList(BOQStatusFilterDataList filterDataList, String field) {
        for (FilterAttributeData filterData : filterDataList.getFilterData()) {
            if (filterData.getAttrName().equalsIgnoreCase(field))
                return filterData.getAttrValue();
        }
        return null;
    }
}
