package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.model.InventoryMonthPriceMapping_;
import com.ec.application.model.Product_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class InventoryMonthPriceMappingSpecification {
    static SpecificationsBuilder<InventoryMonthPriceMapping> specbldr = new SpecificationsBuilder<InventoryMonthPriceMapping>();

    public static Specification<InventoryMonthPriceMapping> getSpecification(FilterDataList filterDataList) {
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        Specification<InventoryMonthPriceMapping> finalSpec = null;
        if (productNames != null && productNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains(InventoryMonthPriceMapping_.PRODUCT,Product_.PRODUCT_NAME, productNames));
        if (categoryNames != null && categoryNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldContains(InventoryMonthPriceMapping_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, categoryNames));
        return finalSpec;
    }
}
