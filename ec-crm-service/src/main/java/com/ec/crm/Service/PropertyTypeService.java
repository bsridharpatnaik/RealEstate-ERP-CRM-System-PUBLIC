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
		if(!pRepo.existsByName(ptype.getName()))
		{
			pRepo.save(ptype);
			return ptype;
		}
		else
		{
			throw new Exception("PropertyType already exists!");
		}
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
		Optional<PropertyType> PropertyForUpdateOpt = pRepo.findById(id);
		PropertyType PropertyForUpdate = PropertyForUpdateOpt.get();
		
		if(!PropertyForUpdateOpt.isPresent())
			throw new Exception("Product not found with productid");
		
		if(!pRepo.existsByName(ptype.getName()) && !ptype.getName().equalsIgnoreCase(PropertyForUpdate.getName()))
		{
			PropertyForUpdate.setName(ptype.getName());
			PropertyForUpdate.setDescription(ptype.getDescription());
			
		}
        else if(ptype.getName().equalsIgnoreCase(PropertyForUpdate.getName()))
        {
  
			PropertyForUpdate.setDescription(ptype.getDescription());
        }
        else 
        {
        	throw new Exception("PropertyType with same Name already exists");
        }
		return pRepo.save(PropertyForUpdate);
	}
	
	public void deletePropertyType(Long id) throws Exception 
	{
		pRepo.softDeleteById(id);
	}
}
