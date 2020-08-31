package com.ec.crm.ReusableClasses;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;

public class SpecificationsBuilder<T>
{

	  //#######################################//
	 //    				 Level 0   		      //
	//#######################################//
	
	
    public Specification<T> whereDirectFieldContains(String key,List<String> names)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.like(root.get(key), "%"+ name  +"%");
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    public Specification<T> whereDirectBoleanFieldEquals(String key,List<String> names)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), Boolean.parseBoolean(name));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    public Specification<T> whereDirectFieldEquals(String key,List<String> names)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), name);
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    
    public Specification<T> whereEnumFieldEquals(String key,List<String> names,Class claz)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		@SuppressWarnings("unchecked")
			Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), getEnum(name, claz));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    
    public Specification<T> whereProperytyEnumFieldEquals(String key,List<String> names)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), PropertyTypeEnum.valueOf(name));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    
    public Specification<T> whereSentimentEnumFieldEquals(String key,List<String> names)
    {
    	Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), SentimentEnum.valueOf(name));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
    }
    
    public Specification<T> whereDirectFieldDateGreaterThan(String key, List<String> startDates) throws ParseException 
    {
    	Date startDate=new SimpleDateFormat("yyyy/MM/dd").parse(startDates.get(0));
    	Specification<T> finalSpec = null;
    	Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	            -> cb.greaterThanOrEqualTo(root.get(key), ReusableMethods.atStartOfDay(startDate));
	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	return finalSpec; 
	}
    
    public Specification<T> whereDirectFieldDateLessThan(String key, List<String> endDates) throws ParseException 
    {
    	Date endDate=new SimpleDateFormat("yyyy/MM/dd").parse(endDates.get(0));
    	Specification<T> finalSpec = null;
    	Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	            -> cb.lessThanOrEqualTo(root.get(key), ReusableMethods.atEndOfDay(endDate));
	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	return finalSpec; 
	}
    
    public Specification<T> whereDirectLongFieldContains(String key, List<String> assignees) 
    {
    	Specification<T> finalSpec = null;
    	for(String name:assignees)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(key), Long.parseLong(name));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
	}
    
	  //#######################################//
	 //     			Level 1			 	  //
	//#######################################//
    
	public Specification<T> whereChildFieldContains(String childTable, String childFiledName,
			List<String> names) 
	{
		Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.like(root.get(childTable).get(childFiledName), "%"+ name  +"%");
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
	}
	
	public Specification<T> whereChildFieldEquals(String childTable, String childFiledName,
			List<String> names) 
	{
		Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(childTable).get(childFiledName), name );
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
	}

	public Specification<T> whereChildFieldDateGreaterThan(String childTable, String childFiledName, List<String> startDates) throws ParseException 
    {
    	Date startDate=new SimpleDateFormat("yyyy/MM/dd").parse(startDates.get(0));
    	Specification<T> finalSpec = null;
    	Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	            -> cb.greaterThanOrEqualTo(root.get(childTable).get(childFiledName), ReusableMethods.atStartOfDay(startDate));
	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	return finalSpec; 
	}
    
    public Specification<T> whereChildFieldDateLessThan(String childTable, String childFiledName,List<String> endDates) throws ParseException 
    {
    	Date endDate=new SimpleDateFormat("yyyy/MM/dd").parse(endDates.get(0));
    	Specification<T> finalSpec = null;
    	Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	            -> cb.lessThanOrEqualTo(root.get(childTable).get(childFiledName), ReusableMethods.atEndOfDay(endDate));
	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	return finalSpec; 
	}
    
    public Specification<T> whereChildLongFieldContains(String childTable, String childFiledName, List<String> assignees) 
    {
    	Specification<T> finalSpec = null;
    	for(String name:assignees)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
    	            -> cb.equal(root.get(childTable).get(childFiledName), Long.parseLong(name));
    	    finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
        return finalSpec;
	}

	  //#######################################//
	 //     			Level 2		 	  //
	//#######################################//
  
	public Specification<T> whereGrandChildFieldContains(String childTable, String grandChildTable,String grandChildFiledName,
			List<String> names) 
	{
		Specification<T> finalSpec = null;
  	for(String name:names)
  	{
  		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
  	            -> cb.like(root.get(childTable).get(grandChildTable).get(grandChildFiledName), "%"+ name  +"%");
  	    finalSpec  = specOrCondition(finalSpec,internalSpec);
  	}
      return finalSpec;
	}
	
	public Specification<T> whereChildFieldListContains(String childTableName, String gcTable,String fieldName, List<String> names) 
	{
		Specification<T> finalSpec = null;
    	for(String name:names)
    	{
    		Specification<T> internalSpec = (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
	            -> cb.like(root.join(childTableName).join(gcTable).get(fieldName), "%"+name+"%" );
	            finalSpec  = specOrCondition(finalSpec,internalSpec);
    	}
		return finalSpec;
	}
	
	  //#######################################//
	 //     Reusable Spec Setter for NULLs    //
	//#######################################//
	
    public Specification<T> specAndCondition(Specification<T> finalSpec, Specification<T> internalSpec) 
	{
		if(finalSpec == null) return internalSpec;
		else return finalSpec.and(internalSpec);
	}
    
    public Specification<T> specOrCondition(Specification<T> finalSpec, Specification<T> internalSpec) 
	{
		if(finalSpec == null) return internalSpec;
		else return finalSpec.or(internalSpec);
	}
    
    
	  //############################################//
	 //     Reusable method to fetch filter Data   //
	//############################################//
    
    public static List<String> fetchValueFromFilterList(FilterDataList filterDataList, String field) 
	{
		List<String> returnValue = null;
		for(FilterAttributeData filterData : filterDataList.getFilterData())
		{
			if(filterData.getAttrName().equalsIgnoreCase(field))
				returnValue = filterData.getAttrValue();
		}
		return returnValue;
	} 
    
    public static <E extends Enum<E>> E getEnum( String text, Class<E> klass){
		   return Enum.valueOf(klass, text );
		}
}