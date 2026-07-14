package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.InventoryReport;
import com.ec.application.model.InventoryReport_;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class InventoryReportSpecification {

    private static final SpecificationsBuilder<InventoryReport> specbldr = new SpecificationsBuilder<>();

    public static Specification<InventoryReport> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> productNames   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "products");
        List<String> categoryNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categories");
        List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouses");
        List<String> month          = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "month");

        Specification<InventoryReport> spec = null;

        if (notEmpty(productNames))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryReport_.PRODUCT_NAME, productNames));

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryReport_.CATEGORY_NAME, categoryNames));

        if (notEmpty(warehouseNames))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryReport_.WAREHOUSE_NAME, warehouseNames));

        if (notEmpty(month))
            spec = and(spec, specbldr.whereDirectFieldEquals(InventoryReport_.MONTH, month));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
