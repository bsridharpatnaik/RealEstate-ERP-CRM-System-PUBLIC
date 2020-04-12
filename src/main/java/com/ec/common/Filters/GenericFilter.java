package com.ec.common.Filters;

import java.util.List;



public class GenericFilter 
{
	public String filter(FilterPayload filterPayload,String table) 
	{
		StringBuilder queryStr = new StringBuilder("Select * from "+table+" where "); // TODO
		int idx = 0;
		for(Filter filter : filterPayload.getFilters()) 
		{
			queryStr.append(idx==0?"":" and ").append(table+".").append(filter.getColumn()).append(" ");
			queryStr.append(filter.getOperator()).append(" '");
			queryStr.append(filter.getValue()).append("'");
			idx++;
		}
		return queryStr.toString();
	}
}
