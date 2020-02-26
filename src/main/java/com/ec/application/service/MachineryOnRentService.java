package com.ec.application.service;

import java.util.Optional;

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
		Optional<Location> locationOpt = locationRepo.findById(payload.getLocationId());
		Optional<Vendor> vendorOpt = vendorRepo.findById(payload.getVendorId());
		Optional<Machinery> machineryOpt = machineryRepo.findById(payload.getMachineryId());
		
		machineryOnRent.setAmountCharged(payload.getAmountCharged());
		machineryOnRent.setStartDate(payload.getStartDate());
		machineryOnRent.setEndDate(payload.getEndDate());
		machineryOnRent.setEndMeterReading(payload.getEndMeterReading());
		machineryOnRent.setInitialMeterReading(payload.getInitialMeterReading());
		machineryOnRent.setLocation(locationOpt.get());
		machineryOnRent.setMachinery(machineryOpt.get());
		machineryOnRent.setMode(payload.getMode());
		machineryOnRent.setNoOfTrips(payload.getNoOfTrips());
		machineryOnRent.setVendor(vendorOpt.get());
		machineryOnRent.setDate(payload.getDate());
		return morRepo.save(machineryOnRent);
	}

	public Page<MachineryOnRent> findAll(Pageable pageable) 
	{
		return morRepo.findAll(pageable);
	}
	

}
