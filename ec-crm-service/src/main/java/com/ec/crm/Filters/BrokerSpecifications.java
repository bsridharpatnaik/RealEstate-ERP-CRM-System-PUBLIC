package com.ec.crm.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Model.Broker;
import com.ec.crm.Model.Broker_;

import lombok.NonNull;

public final class BrokerSpecifications 
{
	public static Specification<Broker> whereBrokernameContains(@NonNull String name) {
        return (Root<Broker> root, CriteriaQuery<?> query, CriteriaBuilder cb)
                -> cb.like(root.get(Broker_.brokerName), "%"+name+"%");
    }
}
