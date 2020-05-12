package com.ec.application.service;

import java.text.ParseException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.data.CreateMORentData;
import com.ec.application.data.MachineryOnRentWithDropdownData;
import com.ec.application.model.Machinery;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.model.Product;
import com.ec.application.model.Supplier;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.MachineryOnRentSpecifications;
import com.ec.common.Filters.ProductSpecifications;

@Service
public class MachineryOnRentService 
{
	@Autowired
	MachineryOnRentRepo morRepo;
	
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	MachineryRepo machineryRepo;
	
	@Autowired
	SupplierRepo supplierRepo;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	
	public MachineryOnRent createData(CreateMORentData payload) throws Exception
	{
		MachineryOnRent machineryOnRent = new MachineryOnRent();
		populateData(machineryOnRent,payload);
		return morRepo.save(machineryOnRent);
		
	}
	public MachineryOnRent UpdateData(CreateMORentData payload,Long id) throws Exception
	{
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if(!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		populateData(machineryOnRent,payload);
		return morRepo.save(machineryOnRent);
		
	}
	
	public MachineryOnRent findById(Long id) throws Exception
	{
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if(!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		return morRepo.save(machineryOnRent);
		
	}
	public void DeleteData(Long id) throws Exception
	{
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if(!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID "+id+" Not found");
		MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		morRepo.softDeleteById(id);
	}
	
	

	private MachineryOnRent populateData(MachineryOnRent machineryOnRent,CreateMORentData payload) throws Exception 
	{
		Optional<UsageLocation> locationOpt = locationRepo.findById(payload.getLocationId());
		Optional<Supplier> supplierOpt = supplierRepo.findById(payload.getContactId());
		Optional<Machinery> machineryOpt = machineryRepo.findById(payload.getMachineryId());
		
		if(!locationOpt.isPresent() || !supplierOpt.isPresent() || !machineryOpt.isPresent())
			throw new Exception("Location/Vendor/Machinery not found");
		machineryOnRent.setAmountCharged(payload.getAmountCharged());
		machineryOnRent.setStartDate(payload.getStartDate());
		machineryOnRent.setEndDate(payload.getEndDate());
		machineryOnRent.setEndMeterReading(payload.getEndMeterReading());
		machineryOnRent.setInitialMeterReading(payload.getInitialMeterReading());
		machineryOnRent.setUsageLocation(locationOpt.get());
		machineryOnRent.setMachinery(machineryOpt.get());
		machineryOnRent.setMode(payload.getMode());
		machineryOnRent.setNoOfTrips(payload.getNoOfTrips());
		machineryOnRent.setSupplier(supplierOpt.get());
		machineryOnRent.setDate(payload.getDate());
		machineryOnRent.setVehicleNo(payload.getVehicleNo());
		machineryOnRent.setAdditionalNotes(payload.getAdditionalNotes());
		return machineryOnRent;
	}

	public MachineryOnRentWithDropdownData findAllWithDropdown(FilterDataList filterDataList, Pageable pageable) throws ParseException 
	{
		MachineryOnRentWithDropdownData morWithDDData = new MachineryOnRentWithDropdownData();
		Specification<MachineryOnRent> spec = MachineryOnRentSpecifications.getSpecification(filterDataList);
		
		if(spec!=null) morWithDDData.setMachineryOnRent(morRepo.findAll(spec,pageable));
		else morWithDDData.setMachineryOnRent(morRepo.findAll(pageable));
		
		morWithDDData.setMorDropdown(populateDropdownService.fetchData("mor"));
		return morWithDDData;
	}
	

}
