package com.ec.crm.Data;


import java.util.List;

import org.springframework.data.domain.Page;

import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.Data;

@Data
public class LeadListWithTypeAheadData 
{
	Page<Lead> LeadDetails;
	NameAndProjectionDataForDropDown dropdownData;
	List<String> typeAheadDataForGlobalSearch;
}
