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
	
	public NameAndProjectionDataForDropDown fetchData() 
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		morDropdownDataList.setVendor(vendorRepo.findIdAndNames());
		morDropdownDataList.setMachinery(machineryRepo.findIdAndNames());
		morDropdownDataList.setLocation(locRepo.findIdAndNames());
		return morDropdownDataList;
		
	}

}
