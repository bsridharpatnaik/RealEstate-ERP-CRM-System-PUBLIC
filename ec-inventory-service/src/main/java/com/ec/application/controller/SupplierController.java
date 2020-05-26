package com.ec.application.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Supplier;
import com.ec.application.service.SupplierService;

@RestController
@RequestMapping("/supplier")
public class SupplierController 
{
	@Autowired
	SupplierService supplierService;
	
	@GetMapping
	public Page<Supplier> returnAllSuppliers(@PageableDefault(page = 0, size = 10, sort = "created", direction = Direction.DESC) Pageable pageable) throws ParseException
	{
		return supplierService.findAll(pageable);
	}
	
	@GetMapping("/names")
	public List<IdNameProjections> returnSupplierNames() 
	{
		return supplierService.getSupplierNames();
	}
	
	@GetMapping("/isused/{id}")
	public Boolean returnSupplierIsUsed(@PathVariable Long id) 
	{
		return supplierService.isContactUsedAsSupplier(id);
	}
}
