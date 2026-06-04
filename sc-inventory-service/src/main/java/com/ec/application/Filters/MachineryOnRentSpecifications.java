package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.model.MachineryOnRent_;
import com.ec.application.model.Machinery_;
import com.ec.application.model.Supplier_;
import com.ec.application.model.UsageLocation_;
import org.springframework.data.jpa.domain.Specification;

import java.text.ParseException;
import java.util.List;

public final class MachineryOnRentSpecifications {

    private static final SpecificationsBuilder<MachineryOnRent> specbldr = new SpecificationsBuilder<>();

    public static Specification<MachineryOnRent> getSpecification(FilterDataList filterDataList) throws ParseException {
        List<String> machineryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "machineryNames");
        List<String> supplierNames  = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "supplierNames");
        List<String> vehicleNos     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "vehicleNos");
        List<String> locations      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageLocationNames");
        List<String> startDates     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
        List<String> endDates       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "EndDate");
        List<String> globalSearch   = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");

        Specification<MachineryOnRent> spec = null;

        if (notEmpty(machineryNames))
            spec = and(spec, specbldr.whereChildFieldContains(MachineryOnRent_.MACHINERY, Machinery_.MACHINERY_NAME, machineryNames));

        if (notEmpty(locations))
            spec = and(spec, specbldr.whereChildFieldContains(MachineryOnRent_.USAGE_LOCATION, UsageLocation_.LOCATION_NAME, locations));

        if (notEmpty(supplierNames))
            spec = and(spec, specbldr.whereChildFieldContains(MachineryOnRent_.SUPPLIER, Supplier_.NAME, supplierNames));

        if (notEmpty(vehicleNos))
            spec = and(spec, specbldr.whereDirectFieldContains(MachineryOnRent_.VEHICLE_NO, vehicleNos));

        if (notEmpty(startDates))
            spec = and(spec, specbldr.whereDirectFieldDateGreaterThan(MachineryOnRent_.DATE, startDates));

        if (notEmpty(endDates))
            spec = and(spec, specbldr.whereDirectFieldDateLessThan(MachineryOnRent_.DATE, endDates));

        if (notEmpty(globalSearch))
            spec = and(spec, specbldr.whereChildFieldContains(MachineryOnRent_.MACHINERY, Machinery_.MACHINERY_NAME, globalSearch));

        return spec;
    }

    private static <T> Specification<T> and(Specification<T> base, Specification<T> next) {
        return base == null ? next : base.and(next);
    }

    private static boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
