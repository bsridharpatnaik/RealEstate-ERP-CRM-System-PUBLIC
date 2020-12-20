package com.ec.application.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.InwardOutwardList_;
import com.ec.application.model.Product;
import com.ec.application.model.Product_;

@Service
@Transactional
public class GroupBySpecification
{
	@Autowired
	EntityManager entityManager;

	Logger log = LoggerFactory.getLogger(GroupBySpecification.class);

	public <T> List<ProductGroupedDAO> findDataByConfiguration(Specification<T> spec, Class entityClazz,
			String joinTable)
	{
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ProductGroupedDAO> query = builder.createQuery(ProductGroupedDAO.class);
		Root<T> root = query.from(entityClazz);
		Predicate p = spec.toPredicate(root, query, builder);
		Join<T, InwardOutwardList> ioList = root.join(joinTable);
		Join<InwardOutwardList, Product> productList = ioList.join(InwardOutwardList_.PRODUCT);
		query.multiselect(productList.get(Product_.PRODUCT_NAME), productList.get(Product_.MEASUREMENT_UNIT),
				builder.sum(ioList.get(InwardOutwardList_.QUANTITY)));
		query.groupBy(productList.get(Product_.PRODUCT_NAME), productList.get(Product_.MEASUREMENT_UNIT));
		query.where(p);
		List<ProductGroupedDAO> groupedData = fetchData(query);
		return groupedData;
	}

	public <E> List<E> fetchData(CriteriaQuery<E> criteria)
	{
		TypedQuery<E> typedQuery = entityManager.createQuery(criteria);
		List<E> resultList = typedQuery.getResultList();
		return resultList;
	}
}
