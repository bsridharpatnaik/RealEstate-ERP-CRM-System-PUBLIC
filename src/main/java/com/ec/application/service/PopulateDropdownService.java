package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.UnloadingAreaRepo;
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
	
	@Autowired
	UnloadingAreaRepo unloadingAreaRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	CategoryRepo categoryRepo;
	
	@Autowired
	LocationRepo locationRepo;

	@Autowired
	ContractorRepo contractorRepo;
	public NameAndProjectionDataForDropDown fetchData(String page) 
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		switch(page)
		{
		//Case machinery on rent
		case "mor":
			morDropdownDataList.setLocation(locRepo.findIdAndNames());
			morDropdownDataList.setMachinery(machineryRepo.findIdAndNames());
			break;
		//case inward inventory
		case "inward":
			morDropdownDataList.setProduct(productRepo.findIdAndNames());
			morDropdownDataList.setUnloadingArea(unloadingAreaRepo.findIdAndNames());
			break;
		case "outward":
			morDropdownDataList.setProduct(productRepo.findIdAndNames());
			morDropdownDataList.setUnloadingArea(unloadingAreaRepo.findIdAndNames());
			morDropdownDataList.setLocation(locationRepo.findIdAndNames());
			morDropdownDataList.setContractor(contractorRepo.findIdAndNames());
			break;
		}
		
		//common for all
		morDropdownDataList.setVendor(vendorRepo.findIdAndNames());
		return morDropdownDataList;
		
	}

}
