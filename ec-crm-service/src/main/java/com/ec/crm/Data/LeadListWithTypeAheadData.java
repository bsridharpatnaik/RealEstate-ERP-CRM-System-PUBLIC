package com.ec.crm.Data;


import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.IdNameProjections;


public class LeadListWithTypeAheadData 
{
	Page<Lead> LeadDetails;
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
	public Page<Lead> getLeadDetails() {
		return LeadDetails;
	}
	public void setLeadDetails(Page<Lead> leadDetails) {
		LeadDetails = leadDetails;
	}
	public NameAndProjectionDataForDropDown getDropdownData() {
		return dropdownData;
	}
	public void setDropdownData(NameAndProjectionDataForDropDown dropdownData) {
		this.dropdownData = dropdownData;
	}
	public List<String> getTypeAheadDataForGlobalSearch() {
		return typeAheadDataForGlobalSearch;
	}
	public void setTypeAheadDataForGlobalSearch(List<String> typeAheadDataForGlobalSearch) {
		this.typeAheadDataForGlobalSearch = typeAheadDataForGlobalSearch;
	}
}
