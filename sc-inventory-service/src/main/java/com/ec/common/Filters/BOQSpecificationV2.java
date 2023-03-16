package com.ec.common.Filters;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQStatusView;
import com.ec.application.model.BOQStatusViewV2;
import com.ec.application.model.BOQStatusViewV2_;
import com.ec.application.model.BOQUploadView_;
import com.ec.application.repository.BOQUploadRepository;
import com.ec.application.repository.InwardOutwardListRepo;
import com.ec.application.repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class BOQSpecificationV2 {

    static SpecificationsBuilder<BOQStatusViewV2> specbldr = new SpecificationsBuilder<BOQStatusViewV2>();

    public static Specification<BOQStatusViewV2> getSpecification(BOQStatusFilterDataList filterDataList) throws Exception {


        List<String> buildingType = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "buildingType");
        List<String> buildingUnits = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "buildingUnit");
        List<String> products = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "product");
        List<String> categories = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "category");
        List<String> consumedPercentages = SpecificationsBuilder.fetchValueFromBoqFilterList(filterDataList, "consumedPercentage");

        Specification<BOQStatusViewV2> finalSpec = null;

        if (products != null && products.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQStatusViewV2_.PRODUCT_NAME, products));

        if (categories != null && categories.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQStatusViewV2_.CATEGORY_NAME, categories));

        if (buildingType != null && buildingType.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQStatusViewV2_.BUILDING_TYPE, buildingType));

        if (buildingUnits != null && buildingUnits.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(BOQStatusViewV2_.LOCATION_NAME, buildingUnits));

        if (consumedPercentages != null && consumedPercentages.size() > 0)
            finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldEquals(BOQStatusViewV2_.STATUS_BUCKET, consumedPercentages));

        return finalSpec;
    }


}
