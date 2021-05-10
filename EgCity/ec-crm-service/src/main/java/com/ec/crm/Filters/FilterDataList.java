package com.ec.crm.Filters;

import java.util.List;

public class FilterDataList {

	private List<FilterAttributeData> filterData;

	public List<FilterAttributeData> getFilterData() {
		return filterData;
	}

	public void setFilterData(List<FilterAttributeData> filterData) {
		this.filterData = filterData;
	}

	@Override
	public String toString() {
		return "FilterDataList [filterData=" + filterData + ", getFilterData()=" + getFilterData() + ", getClass()="
				+ getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}

	
}
