package com.ec.common.Filters;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.ReusableClasses.SpecificationsBuilder;
import com.ec.application.model.Category;
import com.ec.application.model.Category_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

public final class CategorySpecifications 
{
	static SpecificationsBuilder<Category> specbldr = new SpecificationsBuilder<Category>();
	
	public static Specification<Category> getSpecification(FilterDataList filterDataList)
	{
		List<String> categoryNames = specbldr.fetchValueFromFilterList(filterDataList,"name");
		Specification<Category> finalSpec = null;
		if(categoryNames != null && categoryNames.size()>0)
			finalSpec = specbldr.specAndCondition(finalSpec,specbldr.whereDirectFieldContains(Category_.CATEGORY_NAME, categoryNames));	
		return finalSpec;
	}
}
