package com.ec.application.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Contact;
import com.ec.application.model.Contact_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class ContactSpecifications {

    private static final SpecificationsBuilder<Contact> specbldr = new SpecificationsBuilder<>();

    public static Specification<Contact> getSpecification(FilterDataList filterDataList) {
        List<String> names       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "name");
        List<String> types       = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "contacttype");
        List<String> mobile      = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "mobilenumber");
        List<String> address     = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "address");
        List<String> nameOrMobile = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "nameormobile");

        Specification<Contact> spec = null;

        if (notEmpty(names))
            spec = and(spec, specbldr.whereDirectFieldContains(Contact_.name.getName(), names));

        if (notEmpty(types)) {
            if (types.contains("All"))
                expandAllContactTypes(types);
            spec = and(spec, specbldr.whereDirectFieldContains(Contact_.contactType.getName(), types));
        }

        if (notEmpty(mobile))
            spec = and(spec, specbldr.whereDirectFieldContains(Contact_.mobileNo.getName(), mobile));

        if (notEmpty(address)) {
            Specification<Contact> addrSpec = null;
            addrSpec = or(addrSpec, specbldr.whereDirectFieldContains(Contact_.addr_line1.getName(), address));
            addrSpec = or(addrSpec, specbldr.whereDirectFieldContains(Contact_.addr_line2.getName(), address));
            addrSpec = or(addrSpec, specbldr.whereDirectFieldContains(Contact_.city.getName(), address));
            addrSpec = or(addrSpec, specbldr.whereDirectFieldContains(Contact_.state.getName(), address));
            spec = and(spec, addrSpec);
        }

        if (notEmpty(nameOrMobile)) {
            Specification<Contact> nmSpec = null;
            nmSpec = or(nmSpec, specbldr.whereDirectFieldContains(Contact_.name.getName(), nameOrMobile));
            nmSpec = or(nmSpec, specbldr.whereDirectFieldContains(Contact_.mobileNo.getName(), nameOrMobile));
            spec = and(spec, nmSpec);
        }

        return spec;
    }

    private static void expandAllContactTypes(List<String> types) {
        types.clear();
        types.add("SUPPLIER");
        types.add("CONTRACTOR");
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
