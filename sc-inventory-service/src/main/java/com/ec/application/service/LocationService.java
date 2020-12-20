package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.UsageLocationData;
import com.ec.application.model.UsageLocation;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.LocationSpecifications;

@Service
@Transactional
public class LocationService
{
	@Autowired
	LocationRepo locationRepo;

	@Autowired
	BuildingTypeRepo buildingTypeRepo;

	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;

	Logger log = LoggerFactory.getLogger(LocationService.class);

	@Transactional
	public UsageLocation createLocation(UsageLocationData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		validatePayload(payload);
		UsageLocation usageLocation = new UsageLocation();
		setFields(payload, usageLocation);
		if (!locationRepo.existsByLocationName(payload.getLocationName().trim()))
			return locationRepo.save(usageLocation);
		else
			throw new Exception("Location already exists!");
	}

	private void setFields(UsageLocationData payload, UsageLocation usageLocation)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		if (payload.getTypeId() != null)
			usageLocation.setBuildingType(buildingTypeRepo.findById(payload.getTypeId()).get());
		usageLocation.setLocationName(payload.getLocationName());
		usageLocation.setLocationDescription(payload.getLocationDescription());
	}

	private void validatePayload(UsageLocationData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (payload.getLocationName().trim() == "" || payload.getLocationName() == null)
			throw new Exception("Building Unit name cannot be empty");

		if (payload.getTypeId() != null)
		{
			if (!buildingTypeRepo.existsById(payload.getTypeId()))
				throw new Exception("Building Type not found with building type ID");
		}
	}

	public UsageLocation updateLocation(Long id, UsageLocationData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		validatePayload(payload);
		Optional<UsageLocation> LocationForUpdateOpt = locationRepo.findById(id);

		if (!LocationForUpdateOpt.isPresent())
			throw new Exception("Usage location with ID not found.");

		UsageLocation LocationForUpdate = LocationForUpdateOpt.get();
		UsageLocation newLocation = new UsageLocation();
		setFields(payload, newLocation);

		if (!locationRepo.existsByLocationName(newLocation.getLocationName())
				&& !LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
		{
			LocationForUpdate.setLocationName(newLocation.getLocationName().trim());
			LocationForUpdate.setLocationDescription(
					newLocation.getLocationDescription() == null ? "" : newLocation.getLocationDescription().trim());
			if (payload.getTypeId() != null)
				LocationForUpdate.setBuildingType(buildingTypeRepo.getOne(payload.getTypeId()));
			else
				LocationForUpdate.setBuildingType(null);

		} else if (LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
		{
			if (payload.getTypeId() != null)
			{
				LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
				LocationForUpdate.setBuildingType(buildingTypeRepo.getOne(payload.getTypeId()));
			} else
			{
				LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
				LocationForUpdate.setBuildingType(null);
			}
		} else
		{
			throw new Exception("Location with same Name already exists");
		}

		return locationRepo.save(LocationForUpdate);

	}

	public UsageLocation findSingleLocation(Long id)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Optional<UsageLocation> Locations = locationRepo.findById(id);
		return Locations.get();
	}

	public void deleteLocation(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (!checkBeforeDeleteService.isLocationUsed(id))
			locationRepo.softDeleteById(id);
		else
			throw new Exception("Location in use. Cannot delete.");

	}

	public List<IdNameProjections> findIdAndNames()
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return locationRepo.findIdAndNames();
	}

	public Page<UsageLocation> findFilteredLocationsWithTA(FilterDataList filterDataList, Pageable pageable)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Specification<UsageLocation> spec = LocationSpecifications.getSpecification(filterDataList);
		if (spec != null)
			return locationRepo.findAll(spec, pageable);
		else
			return locationRepo.findAll(pageable);
	}

	public List<String> getTypeAheadForGlobalSearch(String str)
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		return locationRepo.getNamesForTypeAhead(str);
	}

}
