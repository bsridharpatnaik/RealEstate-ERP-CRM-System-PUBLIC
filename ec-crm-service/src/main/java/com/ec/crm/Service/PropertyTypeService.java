package com.ec.crm.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Repository.PropertyTypeRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PropertyTypeService {
	@Autowired
	PropertyTypeRepo pRepo;
	public Page<PropertyType> fetchAll(Pageable pageable) 
	{
		return pRepo.findAll(pageable);
	}
	
	public PropertyType createPropertyType(PropertyType ptype) throws Exception {
		pRepo.save(ptype);
		return ptype;
	}
	
	public PropertyType findSinglePropertyType(long id) throws Exception 
	{
		Optional<PropertyType> ptype = pRepo.findById(id);
		if(ptype.isPresent())
			return ptype.get();
		else
			throw new Exception("PropertyType ID not found");
	}
	
	public PropertyType updatePropertyType(Long id, PropertyType ptype) throws Exception 
	{
		pRepo.save(ptype);
	    return ptype;
	}
	
	public void deletePropertyType(Long id) throws Exception 
	{
		pRepo.softDeleteById(id);
	}
}
