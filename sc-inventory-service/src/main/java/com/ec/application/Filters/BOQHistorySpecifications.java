package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQHistory;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class BOQHistorySpecifications {

    static SpecificationsBuilder<BOQHistory> specbldr = new SpecificationsBuilder<>();

    public static Specification<BOQHistory> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> startDate    = specbldr.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDate      = specbldr.fetchValueFromFilterList(filterDataList, "endDate");
        List<String> buildingType = specbldr.fetchValueFromFilterList(filterDataList, "buildingType");
        List<String> buildingUnit = specbldr.fetchValueFromFilterList(filterDataList, "buildingUnit");
        List<String> inventory    = specbldr.fetchValueFromFilterList(filterDataList, "inventory");
        List<String> changeType   = specbldr.fetchValueFromFilterList(filterDataList, "changeType");
        List<String> changedBy    = specbldr.fetchValueFromFilterList(filterDataList, "changedBy");

        Specification<BOQHistory> finalSpec = null;

        if (startDate != null && !startDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateGreaterThan("changeDateTime", startDate));

        if (endDate != null && !endDate.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldDateLessThan("changeDateTime", endDate));

        if (buildingType != null && !buildingType.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains("buildingType", "typeName", buildingType));

        if (buildingUnit != null && !buildingUnit.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains("usageLocation", "locationName", buildingUnit));

        if (inventory != null && !inventory.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereChildFieldContains("product", "productName", inventory));

        if (changeType != null && !changeType.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldEquals("changeType", changeType));

        if (changedBy != null && !changedBy.isEmpty())
            finalSpec = specbldr.specAndCondition(finalSpec,
                    specbldr.whereDirectFieldContains("changedBy", changedBy));

        return finalSpec;
    }
}
