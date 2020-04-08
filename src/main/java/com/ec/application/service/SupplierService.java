package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.model.Supplier;
import com.ec.application.repository.SupplierRepo;

@Service
public class SupplierService 
{

	@Autowired
	SupplierRepo supplierRepo;
	
	public List<IdNameProjections> getSupplierNames()
	{
		List<IdNameProjections> supplierNames = new ArrayList<IdNameProjections>();
		supplierNames = supplierRepo.getSupplierNames();
		return supplierNames;
	}

	public Page<Supplier> findAll(Pageable pageable) 
	{
		// TODO Auto-generated method stub
		return supplierRepo.findAll(pageable);
	}
}
