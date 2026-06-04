package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.constants.BatchMode;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProductSpecifications {

    private static final SpecificationsBuilder<Product> specbldr = new SpecificationsBuilder<>();

    public static Specification<Product> getSpecification(FilterDataList filterDataList) {
        List<String> names         = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> isManagedList = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "isManagedInventory");
        List<String> batchModeList = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "batchModes");
        List<String> productCodes  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productCodes");

        Specification<Product> spec = null;

        if (notEmpty(names)) {
            Specification<Product> nameSpec = null;
            nameSpec = or(nameSpec, specbldr.whereDirectFieldContains(Product_.PRODUCT_NAME, names));
            nameSpec = or(nameSpec, specbldr.whereChildFieldContains(Product_.CATEGORY, Category_.CATEGORY_NAME, names));
            spec = and(spec, nameSpec);
        }

        if (notEmpty(categoryNames))
            spec = and(spec, specbldr.whereChildFieldEquals(Product_.CATEGORY, Category_.CATEGORY_NAME, categoryNames));

        if (notEmpty(isManagedList))
            spec = and(spec, specbldr.whereDirectBoleanFieldEquals(Product_.IS_MANAGED_INVENTORY, isManagedList));

        if (notEmpty(batchModeList)) {
            List<BatchMode> modes = batchModeList.stream()
                    .map(s -> { try { return BatchMode.valueOf(s); } catch (Exception e) { return null; } })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!modes.isEmpty())
                spec = and(spec, (root, query, cb) -> root.get(Product_.BATCH_MODE).in(modes));
        }

        if (notEmpty(productCodes))
            spec = and(spec, specbldr.whereDirectFieldEquals(Product_.PRODUCT_CODE, productCodes));

        return spec;
    }

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
