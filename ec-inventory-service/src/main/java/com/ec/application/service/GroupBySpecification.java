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

@Service
public class GroupBySpecification
{
	@Autowired
	EntityManager entityManager;
	
	 public List<ProductGroupedDAO> findDataByConfiguration(Specification<InwardInventory> spec, List<String> selectColumns, List<String> aggregationColumns, List<String> groupByColumns) 
	 {
		 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
	     CriteriaQuery<ProductGroupedDAO> query = builder.createQuery(ProductGroupedDAO.class);
	     Root<InwardInventory> root = query.from(InwardInventory.class);
		 Predicate p = spec.toPredicate(root, query, builder);
		 ArrayList<Selection<?>> selections = getSelections(selectColumns,aggregationColumns, root, builder);
		 ArrayList<Expression<?>> grouping = getGrouping(groupByColumns,root, builder);
		 query.multiselect(selections);
		 grouping.addAll(grouping);
		 query.groupBy(grouping).where(p);
		 List<ProductGroupedDAO> groupedData = fetchData(query);
		 return groupedData;
	 }

	private ArrayList<Expression<?>> getGrouping(List<String> groupByColumns, Root<InwardInventory> root, CriteriaBuilder builder) 
	{
		ArrayList<Expression<?>> grouping = new ArrayList<Expression<?>>();
		for(String column : groupByColumns)
			grouping.add(root.get(column));
		return grouping;
	}

	private ArrayList<Selection<?>> getSelections(List<String> selectColumns,List<String> aggregationColumns, Root<InwardInventory> root,CriteriaBuilder builder) 
	{
		ArrayList<Selection<?>> selection = new ArrayList<Selection<?>>();
		for(String column : selectColumns)
			selection.add(root.get(column));
		for(String column : aggregationColumns)
			selection.add(builder.sum(root.get(column)));
		return selection;
	}

	public List<ProductGroupedDAO> fetchData(CriteriaQuery<ProductGroupedDAO> criteria) 
	{
		TypedQuery<ProductGroupedDAO> typedQuery = entityManager.createQuery(criteria);
		List<ProductGroupedDAO> resultList = typedQuery.getResultList();
		return resultList;
	}
}
