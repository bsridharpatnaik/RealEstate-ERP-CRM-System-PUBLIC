package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.data.CreateMORentData;
import com.ec.application.model.Location;
import com.ec.application.model.Machinery;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.model.Vendor;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.VendorRepo;

@Service
public class MachineryOnRentService 
{
	@Autowired
	MachineryOnRentRepo morRepo;
	
	@Autowired
	LocationRepo locationRepo;
	
	@Autowired
	VendorRepo vendorRepo;
	
	@Autowired
	MachineryRepo machineryRepo;
	
	public MachineryOnRent createData(CreateMORentData payload)
	{
		MachineryOnRent machineryOnRent = new MachineryOnRent();
		
		machineryOnRent = populateData(payload);
		
		return morRepo.save(machineryOnRent);
		
	}

	private MachineryOnRent populateData(CreateMORentData payload) 
	{
		MachineryOnRent machineryOnRent = new MachineryOnRent();
		Location location = locationRepo.getOne(payload.getLocationId());
		Vendor vendor = vendorRepo.getOne(payload.getVendorId());
		Machinery machinery = machineryRepo.getOne(payload.getMachineryId());
		
		machineryOnRent.setAmountCharged(payload.getAmountCharged());
		machineryOnRent.setEndDate(payload.getEndDate());
		machineryOnRent.setEndMeterReading(payload.getEndMeterReading());
		machineryOnRent.setInitialMeterReading(payload.getInitialMeterReading());
		machineryOnRent.setLocation(location);
		machineryOnRent.setMachinery(machinery);
		machineryOnRent.setMode(payload.getMode());
		machineryOnRent.setNoOfTrips(payload.getNoOfTrips());
		machineryOnRent.setVendor(vendor);
		machineryOnRent.setDate(payload.getDate());
		return morRepo.save(machineryOnRent);
	}

	public Page<MachineryOnRent> findAll(Pageable pageable) 
	{
		// TODO Auto-generated method stub
		return morRepo.findAll(pageable);
	}
	

}
