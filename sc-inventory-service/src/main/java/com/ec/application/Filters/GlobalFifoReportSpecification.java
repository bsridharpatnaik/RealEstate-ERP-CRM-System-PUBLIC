package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.GlobalFifoReport;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class GlobalFifoReportSpecification {

    private static final SpecificationsBuilder<GlobalFifoReport> specbldr = new SpecificationsBuilder<>();

    public static Specification<GlobalFifoReport> getSpecification(FilterDataList filterDataList, List<String> allowedSchemas) throws ParseException {
        List<String> startDate      = specbldr.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate        = specbldr.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> tenantSchema   = specbldr.fetchValueFromFilterList(filterDataList, "tenantSchema");
        List<String> productName    = specbldr.fetchValueFromFilterList(filterDataList, "productName");
        List<String> warehouseName  = specbldr.fetchValueFromFilterList(filterDataList, "warehouseName");
        List<String> performedBy    = specbldr.fetchValueFromFilterList(filterDataList, "performedBy");
        List<String> contractorName = specbldr.fetchValueFromFilterList(filterDataList, "contractorName");

        Specification<GlobalFifoReport> finalSpec = null;

        if (startDate != null && !startDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan("outwardDate", startDate));

        if (endDate != null && !endDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan("outwardDate", endDate));

        if (tenantSchema != null && !tenantSchema.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", tenantSchema));

        if (productName != null && !productName.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("productName", productName));

        if (warehouseName != null && !warehouseName.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("warehouseName", warehouseName));

        if (performedBy != null && !performedBy.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("performedBy", performedBy));

        if (contractorName != null && !contractorName.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("contractorName", contractorName));

        if (allowedSchemas != null && !allowedSchemas.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("tenantSchema", allowedSchemas));

        return finalSpec;
    }
}
