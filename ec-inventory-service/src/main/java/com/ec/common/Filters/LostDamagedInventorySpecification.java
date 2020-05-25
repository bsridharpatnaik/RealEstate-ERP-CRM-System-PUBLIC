package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.LostDamagedInventory;
import com.ec.application.model.LostDamagedInventory_;
import com.ec.application.model.Product_;
import com.ec.application.model.Stock;
import com.ec.application.model.Stock_;
import com.ec.application.model.Warehouse_;

public class LostDamagedInventorySpecification 
{
	static SpecificationsBuilder<LostDamagedInventory> specbldr = new SpecificationsBuilder<LostDamagedInventory>();
	
	public static Specification<LostDamagedInventory> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"EndDate");
		List<String> ProductNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"ProductNames");
		List<String> CategoryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"CategoryNames");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"warehouseNames");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		
		Specification<LostDamagedInventory> finalSpec = null;
		
		if(startDates != null && startDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateGreaterThan(LostDamagedInventory_.DATE, startDates));
		
		if(endDates != null && endDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateLessThan(LostDamagedInventory_.DATE, endDates));
		
		if(ProductNames != null && ProductNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains(LostDamagedInventory_.PRODUCT, Product_.PRODUCT_NAME, ProductNames));
		
		if(CategoryNames != null && CategoryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereGrandChildFieldContains(LostDamagedInventory_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, CategoryNames));
		
		if(warehouseNames != null && warehouseNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains(LostDamagedInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));
		
		
		if(globalSearch != null && globalSearch.size()>0)
		{
			Specification<LostDamagedInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(LostDamagedInventory_.PRODUCT, Product_.PRODUCT_NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereGrandChildFieldContains(LostDamagedInventory_.PRODUCT, Product_.CATEGORY, Category_.CATEGORY_NAME, globalSearch));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
		
		return finalSpec;
	}
}
