package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.MORDropdownData;
import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.VendorRepo;

@Service
public class PopulateDropdownService 
{

	@Autowired
	LocationRepo locRepo;
	
	@Autowired
	MachineryRepo machineryRepo;
	
	@Autowired
	VendorRepo vendorRepo;
	
	public List<NameAndProjectionDataForDropDown> fetchData() 
	{
		List<NameAndProjectionDataForDropDown> morDropdownDataList = new ArrayList<NameAndProjectionDataForDropDown>();
		String[] fields = new String[]{"vendor","machinery","location"};
		for(String field : fields)
		{
			NameAndProjectionDataForDropDown morDropdownData = new NameAndProjectionDataForDropDown();
			morDropdownData = getValues(field);
			morDropdownDataList.add(morDropdownData);
		}
		return morDropdownDataList;
	}

	private NameAndProjectionDataForDropDown getValues(String field) 
	{
		NameAndProjectionDataForDropDown morDropdownData = new NameAndProjectionDataForDropDown();
		switch(field)
		{
			case "vendor":
				morDropdownData.setName("vendor");
				morDropdownData.setProjection(vendorRepo.findIdAndNames());
				break;
			case "machinery":
				morDropdownData.setName("machinery");
				morDropdownData.setProjection(machineryRepo.findIdAndNames());
				break;
			case "location":
				morDropdownData.setName("location");
				morDropdownData.setProjection(locRepo.findIdAndNames());
				break;
		}
		return morDropdownData;
	}

}
