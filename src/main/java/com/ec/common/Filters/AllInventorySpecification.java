package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.AllInventoryTransactions;
import com.ec.application.model.AllInventoryTransactions_;

public class AllInventorySpecification 
{
static SpecificationsBuilder<AllInventoryTransactions> specbldr = new SpecificationsBuilder<AllInventoryTransactions>();
	

	public static Specification<AllInventoryTransactions> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> type = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"type");
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"products");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"warehouses");
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"EndDate");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		
		Specification<AllInventoryTransactions> finalSpec = null;
		
		if(type != null && type.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.TYPE, type));
		
		if(productNames != null && productNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.PRODUCT_NAME, productNames));
		
		if(warehouseNames != null && warehouseNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.WAREHOUSE_NAME, warehouseNames));
		
		if(startDates != null && startDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateGreaterThan(AllInventoryTransactions_.DATE, startDates));
		
		if(endDates != null && endDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateLessThan(AllInventoryTransactions_.DATE, endDates));
		
		if(globalSearch != null && globalSearch.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains(AllInventoryTransactions_.PRODUCT_NAME, productNames));
		
		return finalSpec;
		
		
	}

}
