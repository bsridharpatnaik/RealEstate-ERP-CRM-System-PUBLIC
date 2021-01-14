package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.StringNameDAO;
import com.ec.crm.Model.PropertyName;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Repository.PropertyNameRepo;
import com.ec.crm.Repository.PropertyTypeRepo;

@Service
@Transactional
public class ProjectPlanService
{
	@Autowired
	PropertyTypeRepo ptRepo;

	@Autowired
	PropertyNameRepo pnRepo;

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

		ptRepo.softDeleteById(id);
	}

	public List<PropertyType> getAllPropertyTypes()
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

	private List<PropertyType> populateFields(List<PropertyType> propertyTypes)
	{
		for (PropertyType pt : propertyTypes)
		{
			Set<PropertyName> pnList = pt.getPropertyNames();
			for (PropertyName pn : pnList)
			{
				Random rd = new Random();
				pn.setIsBooked(rd.nextBoolean());
			}
			pt.setBookedProperties(pt.getPropertyNames().stream().filter(c -> c.getIsBooked() == true).count());
			pt.setTotalProperties((long) pt.getPropertyNames().size());
		}
		return propertyTypes;
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
