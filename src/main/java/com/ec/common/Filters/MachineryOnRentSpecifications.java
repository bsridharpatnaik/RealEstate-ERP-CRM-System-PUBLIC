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

public class MachineryOnRentSpecifications 
{

static SpecificationsBuilder<MachineryOnRent> specbldr = new SpecificationsBuilder<MachineryOnRent>();
	
	public static Specification<MachineryOnRent> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> machineryNames = specbldr.fetchValueFromFilterList(filterDataList,"machineryNames");
		List<String> supplierNames = specbldr.fetchValueFromFilterList(filterDataList,"supplierNames");
		List<String> vehicleNos = specbldr.fetchValueFromFilterList(filterDataList,"vehicleNos");
		List<String> locations = specbldr.fetchValueFromFilterList(filterDataList,"locations");
		List<String> startDates = specbldr.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = specbldr.fetchValueFromFilterList(filterDataList,"EndDate");
		
		Specification<MachineryOnRent> finalSpec = null;
		
		if(machineryNames != null && machineryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec, specbldr.whereChildFieldContains
					(MachineryOnRent_.MACHINERY, Machinery_.MACHINERY_NAME, machineryNames));
		
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
		return finalSpec;
	}
}
