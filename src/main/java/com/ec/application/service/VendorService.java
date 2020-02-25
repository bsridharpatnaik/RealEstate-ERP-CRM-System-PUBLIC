package com.ec.application.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.model.Vendor;
import com.ec.application.repository.VendorRepo;

@Service
public class VendorService 
{

	@Autowired
	VendorRepo VendorRepo;
	
	public Page<Vendor> findAll(Pageable pageable)
	{
		return VendorRepo.findAll(pageable);
    }
	
	public Vendor createVendor(Vendor payload) throws Exception 
	{
		if(!VendorRepo.existsByVendorName(payload.getVendorName()))
		{
			VendorRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("Vendor already exists!");
		}
    }

	public Vendor updateVendor(Long id, Vendor payload) throws Exception 
	{
		Optional<Vendor> VendorForUpdateOpt = VendorRepo.findById(id);
        Vendor VendorForUpdate = VendorForUpdateOpt.get();
        
		Vendor newVendor = new Vendor();
        newVendor = payload;
        if(!VendorRepo.existsByVendorName(newVendor.getVendorName()) && 
        		!VendorForUpdate.getVendorName().equalsIgnoreCase(newVendor.getVendorName()))
        {		
        	VendorForUpdate.setVendorName(newVendor.getVendorName());
            VendorForUpdate.setVendorAddress(newVendor.getVendorAddress());
            VendorForUpdate.setVendorEmail(newVendor.getVendorEmail());
            VendorForUpdate.setVendorPhone(newVendor.getVendorPhone());
        }
        else if(VendorForUpdate.getVendorName().equalsIgnoreCase(newVendor.getVendorName()))
        {
        	VendorForUpdate.setVendorAddress(newVendor.getVendorAddress());
            VendorForUpdate.setVendorEmail(newVendor.getVendorEmail());
            VendorForUpdate.setVendorPhone(newVendor.getVendorPhone());
        }
        else 
        {
        	throw new Exception("Vendor with same Name already exists");
        }
        	
        return VendorRepo.save(VendorForUpdate);
        
    }

	public Vendor findSingleVendor(Long id) 
	{
		Optional<Vendor> Vendors = VendorRepo.findById(id);
		return Vendors.get();
	}
	public void deleteVendor(Long id) throws Exception 
	{
		try
		{
			VendorRepo.softDeleteById(id);
		}
		catch(Exception e)
		{
			throw new Exception("Not able to delete Vendor");
		}
	}

	public ArrayList<Vendor> findVendorsByName(String name) 
	{
		ArrayList<Vendor> machinertList = new ArrayList<Vendor>();
		machinertList = VendorRepo.findByVendorName(name);
		return machinertList;
	}

	public ArrayList<Vendor> findVendorsByPartialName(String name) 
	{
		return VendorRepo.findByPartialName(name);
	}
}
