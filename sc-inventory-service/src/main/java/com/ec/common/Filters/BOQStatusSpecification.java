package com.ec.common.Filters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.BOQStatus;
import com.ec.application.model.BOQStatus_;

public class BOQStatusSpecification
{
	static SpecificationsBuilder<BOQStatus> specbldr = new SpecificationsBuilder<BOQStatus>();

	public static Specification<BOQStatus> getSpecification(Long id, FilterDataList filterDataList) throws Exception
	{
		List<String> inventoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "inventoryName");
		List<String> boqStatus = SpecificationsBuilder.fetchValueFromFilterList(filterDataList, "consumedPercent");

		Specification<BOQStatus> finalSpec = null;

		// Filter by LocationID
		List<String> idList = new ArrayList<String>();
		idList.add(id.toString());
		finalSpec = specbldr.specAndCondition(finalSpec,
				specbldr.whereDirectFieldLongFieldContains(BOQStatus_.LOCATION_ID, idList));

		// Filter by inventory name
		if (inventoryNames != null && inventoryNames.size() > 0)
			finalSpec = specbldr.specAndCondition(finalSpec,
					specbldr.whereDirectFieldContains(BOQStatus_.PRODUCT_NAME, inventoryNames));

		if (boqStatus != null && boqStatus.size() > 0)
		{
			try
			{
				finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDoubleGreaterThan(
						BOQStatus_.CONSUMED_PERCENT, Double.parseDouble(boqStatus.get(0))));
			} catch (Exception e)
			{
				throw new Exception("Unable to parse number value from string");
			}
		}
		return finalSpec;
	}
}
