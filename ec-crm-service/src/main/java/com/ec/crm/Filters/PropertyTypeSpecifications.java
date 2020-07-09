package com.ec.crm.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.PropertyType_;
import com.ec.crm.Model.Source;
import com.ec.crm.Model.Source_;

import lombok.NonNull;

public final class PropertyTypeSpecifications 
{
	public static Specification<PropertyType> wherePropertynameContains(@NonNull String name) {
        return (Root<PropertyType> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(PropertyType_.name), "%"+name+"%");
    }
}
