package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Machinery;
import com.ec.application.model.Machinery_;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class MissingInventoryPricingSpecifications {
    static SpecificationsBuilder<Machinery> specbldr = new SpecificationsBuilder<Machinery>();

    public static Specification<Machinery> getSpecification(FilterDataList filterDataList)
    {
        List<String> machineryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"name");
        Specification<Machinery> finalSpec = null;
        if(machineryNames != null && machineryNames.size()>0)
            finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Machinery_.MACHINERY_NAME, machineryNames));
        return finalSpec;
    }
    
}
