package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Repository.AddressRepo;

import lombok.extern.slf4j.Slf4j;

import com.ec.crm.Model.Address;
@Service
@Slf4j
public class AddressService {
	@Autowired
	AddressRepo aRepo;
	public Page<Address> fetchAll(Pageable pageable) 
	{
		return aRepo.findAll(pageable);
	}
	
	public Address createAddress(Address addressData) throws Exception {
		aRepo.save(addressData);
		return addressData;
	}
	
	public Address findSingleAddress(long id) throws Exception 
	{
		Optional<Address> address = aRepo.findById(id);
		if(address.isPresent())
			return address.get();
		else
			throw new Exception("address ID not found");
	}
	
	public Address updateAddress(Long id, Address address) throws Exception 
	{
		aRepo.save(address);
	    return address;
	}
	
	public void deleteAddress(Long id) throws Exception 
	{
		aRepo.softDeleteById(id);
	}
}
