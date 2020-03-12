package com.ec.application.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.MORDropdownData;
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
	
	public ArrayList<MORDropdownData> fetchData() 
	{
		ArrayList<MORDropdownData> morDropdownDataList = new ArrayList<MORDropdownData>();
		String[] fields = new String[]{"vendor","machinery","location"};
		for(String field : fields)
		{
			MORDropdownData morDropdownData = new MORDropdownData();
			morDropdownData = getValues(field);
			morDropdownDataList.add(morDropdownData);
		}
		return morDropdownDataList;
	}

	private MORDropdownData getValues(String field) 
	{
		MORDropdownData morDropdownData = new MORDropdownData();
		switch(field)
		{
			case "vendor":
				morDropdownData.setField("vendor");
				morDropdownData.setValues(vendorRepo.findIdAndNames());
				break;
			case "machinery":
				morDropdownData.setField("machinery");
				morDropdownData.setValues(machineryRepo.findIdAndNames());
				break;
			case "location":
				morDropdownData.setField("location");
				morDropdownData.setValues(locRepo.findIdAndNames());
				break;
		}
		return morDropdownData;
	}

}
