package com.ec.common.Filters;

import java.util.List;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

public final class ProductSpecifications 
{
	static SpecificationsBuilder<Product> specbldr = new SpecificationsBuilder<Product>();
	
	public static Specification<Product> getSpecification(FilterDataList filterDataList)
	{
		List<String> names = specbldr.fetchValueFromFilterList(filterDataList,"name");
		Specification<Product> finalSpec = null;
		if(names != null && names.size()>0)
		{
			finalSpec = specbldr.specOrCondition(finalSpec, specbldr.whereDirectFieldContains(Product_.PRODUCT_NAME, names));
			finalSpec = specbldr.specOrCondition(finalSpec,
					specbldr.whereChildFieldContains(Product_.CATEGORY,Category_.CATEGORY_NAME, names));
		}	
		return finalSpec;	
	}
}
