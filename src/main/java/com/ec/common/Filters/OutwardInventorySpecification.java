package com.ec.common.Filters;

import java.text.ParseException;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.InwardOutwardList_;
import com.ec.application.model.OutwardInventory;
import com.ec.application.model.Product_;
import com.ec.application.model.Supplier_;
import com.ec.application.model.Warehouse_;

public final class OutwardInventorySpecification 
{

	static SpecificationsBuilder<OutwardInventory> specbldr = new SpecificationsBuilder<OutwardInventory>();
	public static Specification<OutwardInventory> getSpecification(FilterDataList filterDataList) throws ParseException
	{
		
		Specification<OutwardInventory> finalSpec = null;	
		return finalSpec;
	}
}
