package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.NameAndProjectionDataForDropDown;
import com.ec.application.repository.CategoryRepo;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.application.repository.WarehouseRepo;
@Service
public class PopulateDropdownService 
{

	@Autowired
	LocationRepo locRepo;
	
	@Autowired
	MachineryRepo machineryRepo;
	
	@Autowired
	WarehouseRepo warehouseRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	CategoryRepo categoryRepo;
	
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	SupplierRepo supplierRepo;
	
	@Autowired
	ContractorRepo contractorRepo;

	public NameAndProjectionDataForDropDown fetchData(String page) 
	{
		NameAndProjectionDataForDropDown morDropdownDataList = new NameAndProjectionDataForDropDown();
		switch(page)
		{
		//Case machinery on rent
		case "mor":
			morDropdownDataList.setUsagelocation(locationRepo.findIdAndNames());
			morDropdownDataList.setMachinery(machineryRepo.findIdAndNames());
			morDropdownDataList.setSupplier(supplierRepo.getSupplierNames());
			break;
		//case inward inventory
		case "inward":
			morDropdownDataList.setProduct(productRepo.findIdAndNames());
			morDropdownDataList.setWarehouse(warehouseRepo.findIdAndNames());
			morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
			break;
		case "outward":
			morDropdownDataList.setProduct(productRepo.findIdAndNames());
			morDropdownDataList.setCategory(categoryRepo.findIdAndNames());
			morDropdownDataList.setUsagelocation(locationRepo.findIdAndNames());
			morDropdownDataList.setContractor(contractorRepo.findIdAndNames());
			break;
		}
		return morDropdownDataList;
		
	}

}
