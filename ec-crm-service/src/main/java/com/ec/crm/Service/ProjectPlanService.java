package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.PropertyTypeReturnDAO;
import com.ec.crm.Data.StringNameDAO;
import com.ec.crm.Model.PropertyName;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Repository.PropertyNameRepo;
import com.ec.crm.Repository.PropertyTypeRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

@Service
@Transactional
public class ProjectPlanService
{
	@Autowired
	PropertyTypeRepo ptRepo;

	@Autowired
	PropertyNameRepo pnRepo;

	@Autowired
	CheckBeforeDeleteService cbdService;

	public PropertyType createNew(StringNameDAO payload) throws Exception
	{
		if (ptRepo.existsByPropertyType(payload.getName()))
			throw new Exception("Property Type already exists. Please try with a different name");

		return ptRepo.save(new PropertyType(payload.getName(), new HashSet<PropertyName>()));
	}

	public PropertyType editExisting(Long id, StringNameDAO payload) throws Exception
	{
		if (!ptRepo.existsById(id))
			throw new Exception("Property Type dosen't exists with ID - " + id);

		PropertyType pt = ptRepo.findById(id).get();
		if (!pt.getPropertyType().equals(payload.getName()))
		{
			if (ptRepo.existsByPropertyType(payload.getName()))
				throw new Exception("Property Type already exists with name - " + payload.getName()
						+ " . Please try with a different name");
		}
		pt.setPropertyType(payload.getName());
		return ptRepo.save(pt);
	}

	public void deletePropertyType(Long id) throws Exception
	{
		if (!ptRepo.existsById(id))
			throw new Exception("Property Type dosen't exists with ID - " + id);

		if (cbdService.isPropertyTypeUsed(id))
			throw new Exception("Cannot delete property type. Already tagged to Deal Structure");

		ptRepo.softDeleteById(id);
	}

	public List<PropertyTypeReturnDAO> getAllPropertyTypes()
	{

		// return ptRepo.findAll();
		List<PropertyType> ptList = ptRepo.findAll();
		for (PropertyType pt : ptList)
		{
			Set<PropertyName> pnList = pt.getPropertyNames();
			Set<PropertyName> pnSortedList = pnList.stream().sorted(Comparator.comparing(PropertyName::getName))
					.collect(Collectors.toSet());
			pt.setPropertyNames(pnSortedList);
		}
		return populateFields(ptList.stream().sorted(Comparator.comparing(PropertyType::getPropertyType))
				.collect(Collectors.toList()));
	}

	private List<PropertyTypeReturnDAO> populateFields(List<PropertyType> propertyTypes)
	{
		List<PropertyTypeReturnDAO> returnData = new ArrayList<PropertyTypeReturnDAO>();
		for (PropertyType pt : propertyTypes)
		{
			PropertyTypeReturnDAO obj = new PropertyTypeReturnDAO();
			
			List<PropertyName> propNamesList = ReusableMethods.convertSetToList(pt.getPropertyNames());
			propNamesList.sort(
					(o1,o2)->{
			            if(o1.getName().length() > o2.getName().length())      return 1;
			            else if(o1.getName().length() < o2.getName().length()) return -1;
			            else return o1.getName().compareTo(o2.getName());
			        }
					);
			obj.setBookedProperties(pt.getPropertyNames().stream().filter(c -> c.getIsBooked() == true).count());
			obj.setTotalProperties((long) pt.getPropertyNames().size());
			obj.setPropertyNames(propNamesList);
			obj.setPropertyType(pt.getPropertyType());
			obj.setPropertyTypeId(pt.getPropertyTypeId());
			returnData.add(obj);
		}
		return returnData;
	}

	public PropertyType getSingle(Long id) throws Exception
	{
		if (!ptRepo.existsById(id))
			throw new Exception("Property Type dosen't exists with ID - " + id);
		return ptRepo.findById(id).get();
	}

	public PropertyType addBuilding(Long typeId, StringNameDAO name) throws Exception
	{
		validatePayload(typeId, name.getName());
		PropertyType pt = ptRepo.findById(typeId).get();
		Set<PropertyName> names = pt.getPropertyNames();
		for(PropertyName nam : names)
		{
			if(nam.getName().equals(name.getName()))
				throw new Exception("Property with name - "+name.getName()+" already exists");
		}
			
		names.add(new PropertyName(name.getName()));
		pt.setPropertyNames(names);
		return ptRepo.save(pt);
	}

	public PropertyName aEditBuilding(Long id, StringNameDAO name) throws Exception
	{
		if (!pnRepo.existsById(id))
			throw new Exception("Property Name dosen't exists with ID - " + id);

		if (name.getName().length() > 10)
			throw new Exception("Name should be less than 10 characters");

		PropertyName pn = pnRepo.findById(id).get();
		List<PropertyType> ptList = ptRepo.getPTbyPNID(pn.getPropertyNameId());
		
		if(!pn.getName().equals(name.getName()))
		{
			for (PropertyType pt : ptList)
			{
				Set<PropertyName> pnList = pt.getPropertyNames();
				for(PropertyName pn1:pnList)
				{
					if(pn1.getName().equals(name.getName()))
					{
						throw new Exception("Property Type already exists with name - " + name.getName());
					}
				}
			}		
		}
			
		pn.setName(name.getName());
		return pnRepo.save(pn);
	}

	public void deleteBuilding(Long id) throws Exception
	{

		List<PropertyType> ptList = ptRepo.getPTbyPNID(id);
		for (PropertyType pt : ptList)
		{

			Iterator<PropertyName> it = pt.getPropertyNames().iterator();
			List<PropertyName> pnList = new ArrayList<PropertyName>();
			while (it.hasNext())
			{
				PropertyName pn = it.next();
				if (pn.getPropertyNameId().equals(id))
				{
					if (cbdService.isPropertyNameUsed(id))
						throw new Exception("Cannot delete property. Property already tagged to Deal Structure.");
					pnList.add(pn);
				}
			}
			pt.getPropertyNames().removeAll(pnList);
			ptRepo.save(pt);
		}
	}

	private void validatePayload(Long typeId, String name) throws Exception
	{
		if (!ptRepo.existsById(typeId))
			throw new Exception("Property Type dosen't exists with ID - " + typeId);

	}
}
