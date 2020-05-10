package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Contractor_;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.InwardOutwardList_;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.OutwardInventory_;
import com.ec.application.model.Product_;
import com.ec.application.model.Supplier_;
import com.ec.application.model.UsageLocation_;
import com.ec.application.model.Warehouse_;

public final class OutwardInventorySpecification 
{

	static SpecificationsBuilder<OutwardInventory> specbldr = new SpecificationsBuilder<OutwardInventory>();
	
	public static Specification<OutwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"endDate");
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"productNames");
		List<String> contractorNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"contractorNames");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"warehouseNames");
		List<String> usageLocations = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"usageLocations");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		
		Specification<OutwardInventory> finalSpec = null;
		
		if(startDates != null && startDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateGreaterThan(OutwardInventory_.DATE, startDates));	
		
		if(endDates != null && endDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateLessThan(OutwardInventory_.DATE, endDates));
		
		if(productNames != null && productNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldListContains(
					OutwardInventory_.INWARD_OUTWARD_LIST,InwardOutwardList_.PRODUCT,Product_.PRODUCT_NAME,productNames));
		
		if(contractorNames != null && contractorNames.size()>0)
				finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, contractorNames));	
		
		if(warehouseNames != null && warehouseNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(OutwardInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));	
	
		if(usageLocations != null && usageLocations.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(OutwardInventory_.USAGE_LOCATION, UsageLocation_.LOCATION_NAME, usageLocations));	
		
		if(globalSearch != null && globalSearch.size()>0)
		{
			Specification<OutwardInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(OutwardInventory_.CONTRACTOR, Contractor_.NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereChildFieldListContains(
					OutwardInventory_.INWARD_OUTWARD_LIST,InwardOutwardList_.PRODUCT,Product_.PRODUCT_NAME,productNames));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
			
			
		return finalSpec;
	}
}
