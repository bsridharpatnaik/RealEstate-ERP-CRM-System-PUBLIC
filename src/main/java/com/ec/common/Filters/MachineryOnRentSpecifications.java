package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.model.MachineryOnRent_;
import com.ec.application.model.Machinery_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;
import com.ec.application.model.Supplier;
import com.ec.application.model.Supplier_;
import com.ec.application.model.UsageLocation_;

public class MachineryOnRentSpecifications 
{

static SpecificationsBuilder<MachineryOnRent> specbldr = new SpecificationsBuilder<MachineryOnRent>();
	

	public static Specification<MachineryOnRent> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> machineryNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"machineryNames");
		List<String> supplierNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"supplierNames");
		List<String> vehicleNos = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"vehicleNos");
		List<String> locations = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"usageLocationNames");
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"EndDate");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		
		Specification<MachineryOnRent> finalSpec = null;
		
		if(machineryNames != null && machineryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains
					(MachineryOnRent_.MACHINERY, Machinery_.MACHINERY_NAME, machineryNames));
		
		if(locations != null && locations.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains
					(MachineryOnRent_.USAGE_LOCATION, UsageLocation_.LOCATION_NAME, locations));
		
		if(supplierNames != null && supplierNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains
					(MachineryOnRent_.SUPPLIER, Supplier_.NAME, supplierNames));
		
		if(vehicleNos != null && vehicleNos.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldContains
					(MachineryOnRent_.VEHICLE_NO, vehicleNos));
		
		if(startDates != null && startDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateGreaterThan
					(MachineryOnRent_.START_DATE, startDates));
		
		if(endDates != null && endDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereDirectFieldDateLessThan
					(MachineryOnRent_.END_DATE, endDates));
		
		if(globalSearch != null && globalSearch.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains
					(MachineryOnRent_.MACHINERY, Machinery_.MACHINERY_NAME, globalSearch));
		
		return finalSpec;
	}
}
