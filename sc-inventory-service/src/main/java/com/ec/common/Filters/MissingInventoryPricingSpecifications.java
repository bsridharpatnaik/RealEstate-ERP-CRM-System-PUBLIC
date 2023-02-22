package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class MissingInventoryPricingSpecifications {
    static SpecificationsBuilder<MissingInventoryPricing> specbldr = new SpecificationsBuilder<MissingInventoryPricing>();

    public static Specification<MissingInventoryPricing> getSpecification(FilterDataList filterDataList) {
        List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
        List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
        Specification<MissingInventoryPricing> finalSpec = null;
        if (productNames != null && productNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(MissingInventoryPricing_.PRODUCT_NAME, productNames));
        if (categoryNames != null && categoryNames.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(MissingInventoryPricing_.CATEGORY_NAME, categoryNames));
        return finalSpec;
    }
}
