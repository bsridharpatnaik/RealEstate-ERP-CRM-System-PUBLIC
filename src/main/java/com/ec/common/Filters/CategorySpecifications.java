package com.ec.common.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.application.model.Category;
import com.ec.application.model.Category_;

import lombok.NonNull;

public final class CategorySpecifications 
{

	public static Specification<Category> whereCategoryNameContains(@NonNull String categoryName) 
	{
        return (Root<Category> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(Category_.CATEGORY_NAME), "%"+categoryName+"%");
    }
}
