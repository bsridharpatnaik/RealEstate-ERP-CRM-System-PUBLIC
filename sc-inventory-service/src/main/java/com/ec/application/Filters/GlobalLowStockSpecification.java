package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.GlobalLowStockReport;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class GlobalLowStockSpecification {

    private static final SpecificationsBuilder<GlobalLowStockReport> specbldr = new SpecificationsBuilder<>();

    public static Specification<GlobalLowStockReport> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> tenantSchema = specbldr.fetchValueFromFilterList(filterDataList, "tenantSchema");
        List<String> category     = specbldr.fetchValueFromFilterList(filterDataList, "category");
        List<String> productName  = specbldr.fetchValueFromFilterList(filterDataList, "productName");
        List<String> startDate    = specbldr.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate      = specbldr.fetchValueFromFilterList(filterDataList, "endDate");

        Specification<GlobalLowStockReport> finalSpec = null;

        if (tenantSchema != null && !tenantSchema.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", tenantSchema));

        if (category != null && !category.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("category", category));

        if (productName != null && !productName.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("productName", productName));

        // startDate/endDate filter on lowStockSince
        if (startDate != null && !startDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan("lowStockSince", startDate));

        if (endDate != null && !endDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan("lowStockSince", endDate));

        return finalSpec;
    }
}
