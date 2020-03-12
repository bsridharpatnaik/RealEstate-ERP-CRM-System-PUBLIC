package com.ec.application.data;

import java.util.List;

import com.ec.application.Projections.IdNameProjections;

public class NameAndProjectionDataForDropDown 
{

	String name;
	List<IdNameProjections> projection;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<IdNameProjections> getProjection() {
		return projection;
	}
	public void setProjection(List<IdNameProjections> projection) {
		this.projection = projection;
	}
	
	
}

