package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.ProductGroupedDAO;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardInventory_;
import com.ec.application.model.InwardOutwardList_;
import com.ec.application.model.Product_;

@Service
public class GroupBySpecification
{
	@Autowired
	EntityManager entityManager;
	
	 public <T,E> List<E> findDataByConfiguration(Specification<T> spec,Class entityClazz, Class daoClass, List<String> selectColumns, 
			 List<String> aggregationColumns, List<String> groupByColumns,String parent,String grandParent) 
	 {
		 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
	     CriteriaQuery<E> query = builder.createQuery(daoClass);
	     @SuppressWarnings("unchecked")
		 Root<T> root = query.from(entityClazz);
		 Predicate p = spec.toPredicate(root, query, builder);
		 ArrayList<Selection<?>> selections = getSelections(selectColumns,aggregationColumns, root, builder,parent,grandParent);
		 ArrayList<Expression<?>> grouping = getGrouping(groupByColumns,root, builder,parent,grandParent);
		 query.multiselect(selections);
		 grouping.addAll(grouping);
		 query.groupBy(grouping).where(p);
		 List<E> groupedData = fetchData(query);
		 return groupedData;
	 }
	 
	private <T> ArrayList<Expression<?>> getGrouping(List<String> groupByColumns, Root<T> root, CriteriaBuilder builder,String parent,String grandParent) 
	{
		ArrayList<Expression<?>> grouping = new ArrayList<Expression<?>>();
		for(String column : groupByColumns)
			grouping.add(root.get(grandParent).get(parent).get(column));
		return grouping;
	}

	private <T> ArrayList<Selection<?>> getSelections(List<String> selectColumns,List<String> aggregationColumns, Root<T> root,CriteriaBuilder builder,String parent,String grandParent) 
	{
		ArrayList<Selection<?>> selection = new ArrayList<Selection<?>>();
		for(String column : selectColumns)
			selection.add(root.get(InwardInventory_.INWARD_OUTWARD_LIST).get(InwardOutwardList_.PRODUCT).get(Product_.PRODUCT_NAME));
		for(String column : aggregationColumns)
			selection.add(builder.sum(root.get(InwardInventory_.INWARD_OUTWARD_LIST).get(InwardOutwardList_.QUANTITY)));
			
			//selection.add(builder.sum(root.get(grandParent).get(parent).get(column)));
		return selection;
	}

	public <E>List<E> fetchData(CriteriaQuery<E> criteria) 
	{
		TypedQuery<E> typedQuery = entityManager.createQuery(criteria);
		List<E> resultList = typedQuery.getResultList();
		return resultList;
	}
}
