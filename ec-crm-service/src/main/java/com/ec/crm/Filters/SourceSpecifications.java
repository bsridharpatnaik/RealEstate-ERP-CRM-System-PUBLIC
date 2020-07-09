package com.ec.crm.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Model.Source;
import com.ec.crm.Model.Source_;

import lombok.NonNull;

public final class SourceSpecifications 
{
	public static Specification<Source> whereSourcenameContains(@NonNull String name) {
        return (Root<Source> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(Source_.sourceName), "%"+name+"%");
    }
}
