package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.GlobalStockAgingReport;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class GlobalStockAgingSpecification {

    private static final SpecificationsBuilder<GlobalStockAgingReport> specbldr = new SpecificationsBuilder<>();

    public static Specification<GlobalStockAgingReport> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> tenantSchema  = specbldr.fetchValueFromFilterList(filterDataList, "tenantSchema");
        List<String> productName   = specbldr.fetchValueFromFilterList(filterDataList, "productName");
        List<String> category      = specbldr.fetchValueFromFilterList(filterDataList, "category");
        List<String> agingBucket   = specbldr.fetchValueFromFilterList(filterDataList, "agingBucket");

        Specification<GlobalStockAgingReport> finalSpec = null;

        if (tenantSchema != null && !tenantSchema.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", tenantSchema));

        if (productName != null && !productName.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("productName", productName));

        if (category != null && !category.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("category", category));

        if (agingBucket != null && !agingBucket.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("agingBucket", agingBucket));

        return finalSpec;
    }
}
