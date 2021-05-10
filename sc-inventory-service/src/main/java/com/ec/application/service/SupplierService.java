package com.ec.application.service;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Supplier;
import com.ec.application.repository.SupplierRepo;

@Service
@Transactional
public class SupplierService
{

	@Autowired
	SupplierRepo supplierRepo;

	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;

	Logger log = LoggerFactory.getLogger(SupplierService.class);

	public List<IdNameProjections> getSupplierNames()
	{
		List<IdNameProjections> supplierNames = new ArrayList<IdNameProjections>();
		supplierNames = supplierRepo.findIdAndNames();
		return supplierNames;
	}

	public Page<Supplier> findAll(Pageable pageable)
	{
		// TODO Auto-generated method stub
		return supplierRepo.findAll(pageable);
	}

	public boolean isContactUsedAsSupplier(Long id)
	{
		boolean isContactUsed = false;
		if (checkBeforeDeleteService.isSupplierUsed(id))
			isContactUsed = true;
		return isContactUsed;
	}
}
