package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import javax.persistence.criteria.SetJoin;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category;
import com.ec.application.model.Category_;
import com.ec.application.model.Contractor_;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.InwardOutwardList_;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.OutwardInventory_;
import com.ec.application.model.Product_;
import com.ec.application.model.Supplier_;
import com.ec.application.model.Warehouse_;

public final class InwardInventorySpecification 
{
	static SpecificationsBuilder<InwardInventory> specbldr = new SpecificationsBuilder<InwardInventory>();
	
	public static Specification<InwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		List<String> startDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"startDate");
		List<String> endDates = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"endDate");
		List<String> productNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"productNames");
		List<String> supplierNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"supplierNames");
		List<String> warehouseNames = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"warehouseNames");
		List<String> invoiceReceived = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"invoiceReceived");
		List<String> globalSearch = SpecificationsBuilder.fetchValueFromFilterList(filterDataList,"globalSearch");
		Specification<InwardInventory> finalSpec = null;
		
		if(startDates != null && startDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateGreaterThan(InwardInventory_.DATE, startDates));	
		
		if(endDates != null && endDates.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldDateLessThan(InwardInventory_.DATE, endDates));
		
		if(productNames != null && productNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldListContains(
					InwardInventory_.INWARD_OUTWARD_LIST,InwardOutwardList_.PRODUCT,Product_.PRODUCT_NAME,productNames));
		
		if(supplierNames != null && supplierNames.size()>0)
				finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, supplierNames));	
		
		if(warehouseNames != null && warehouseNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereChildFieldContains(InwardInventory_.WAREHOUSE, Warehouse_.WAREHOUSE_NAME, warehouseNames));	
		
		if(invoiceReceived != null && invoiceReceived.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectBoleanFieldEquals(InwardInventory_.INVOICE_RECEIVED,invoiceReceived));
	
		if(globalSearch != null && globalSearch.size()>0)
		{
			Specification<InwardInventory> internalSpec = null;
			internalSpec = specbldr.specOrCondition(internalSpec, specbldr.whereChildFieldContains(InwardInventory_.SUPPLIER, Supplier_.NAME, globalSearch));
			internalSpec = specbldr.specOrCondition(internalSpec,specbldr.whereChildFieldListContains(
					InwardInventory_.INWARD_OUTWARD_LIST,InwardOutwardList_.PRODUCT,Product_.PRODUCT_NAME,globalSearch));
			finalSpec = specbldr.specAndCondition(finalSpec,internalSpec);
		}
		return finalSpec;
	}

}
