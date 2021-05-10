package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.ec.application.model.*;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;

public final class OutwardInventorySpecification
{

	static SpecificationsBuilder<OutwardInventory> specbldr = new SpecificationsBuilder<OutwardInventory>();

	public static Specification<OutwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "endDate");
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "productNames");
		List<String> categoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "categoryNames");
		List<String> contractorNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"contractorNames");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "warehouseNames");
		List<String> usageLocations = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageLocation");
		List<String> usageAreas = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "usageArea");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "globalSearch");
		List<String> showOnlyReturned = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"showOnlyReturned");
		List<String> showOnlyRejected = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,
				"showOnlyRejected");
		Specification<OutwardInventory> finalSpec = null;

		if (startDates != null && startDates.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateGreaterThan(OutwardInventory_.DATE, startDates));

		if (endDates != null && endDates.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldDateLessThan(OutwardInventory_.DATE, endDates));

		if (productNames != null && productNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereProductContains(productNames, OutwardInventory_.INWARD_OUTWARD_LIST));
		if (categoryNames != null && categoryNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereCategoryContains(categoryNames, OutwardInventory_.INWARD_OUTWARD_LIST));
		if (contractorNames != null && contractorNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, contractorNames));

		if (warehouseNames != null && warehouseNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr
					.whereChildFieldContains(OutwardInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));

		if (usageLocations != null && usageLocations.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains(
					OutwardInventory_.USAGE_LOCATION, UsageLocation_.LOCATION_NAME, usageLocations));

		if (usageAreas != null && usageAreas.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr
					.whereChildFieldContains(OutwardInventory_.USAGE_AREA, UsageArea_.USAGE_AREA_NAME, usageAreas));

		if (globalSearch != null && globalSearch.size() > 0)
		{
			Specification<OutwardInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,
					specbldr.whereProductContains(globalSearch, OutwardInventory_.INWARD_OUTWARD_LIST));
			finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
		}

		if (showOnlyRejected != null && showOnlyRejected.size() == 1)
		{
			if (showOnlyRejected.get(0).toLowerCase().equals("true"))
			{
				Specification<OutwardInventory> internalSpec = (Root<OutwardInventory> root, CriteriaQuery<?> query,
						CriteriaBuilder cb) -> cb.greaterThan(cb.size(root.get(OutwardInventory_.REJECT_OUTWARD_LIST)),
								0);
				// cb.like(root.get(childTable).get(childFiledName), "%" + name + "%");
				finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
			}
		}
		if (showOnlyReturned != null && showOnlyReturned.size() == 1)
		{
			if (showOnlyReturned.get(0).toLowerCase().equals("true"))
			{
				Specification<OutwardInventory> internalSpec = (Root<OutwardInventory> root, CriteriaQuery<?> query,
						CriteriaBuilder cb) -> cb.greaterThan(cb.size(root.get(OutwardInventory_.RETURN_OUTWARD_LIST)),
								0);
				// cb.like(root.get(childTable).get(childFiledName), "%" + name + "%");
				finalSpec = specbldr.specAndCondition(finalSpec, internalSpec);
			}
		}
		return finalSpec;
	}
}
