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
import com.ec.application.model.UsageLocation;
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
	CheckBeforeDeleteService checkBeforeDeleteService;

	Logger logger = LoggerFactory.getLogger(AllInventoryService.class);

	public UsageLocation createLocation(UsageLocation payload) throws Exception
	{
		if (payload.getLocationName() == "" || payload.getLocationName() == null)
			throw new Exception("Building Unit name cannot be empty");

		if (!locationRepo.existsByLocationName(payload.getLocationName().trim()))
		{
			locationRepo.save(payload);
			return payload;
		} else
		{
			throw new Exception("Location already exists!");
		}
	}

	public UsageLocation updateLocation(Long id, UsageLocation payload) throws Exception
	{
		Optional<UsageLocation> LocationForUpdateOpt = locationRepo.findById(id);
		UsageLocation LocationForUpdate = LocationForUpdateOpt.get();

		UsageLocation newLocation = new UsageLocation();
		newLocation = payload;
		if (!locationRepo.existsByLocationName(newLocation.getLocationName())
				&& !LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
		{
			LocationForUpdate.setLocationName(newLocation.getLocationName().trim());
			LocationForUpdate.setLocationDescription(
					newLocation.getLocationDescription() == null ? "" : newLocation.getLocationDescription().trim());

		} else if (LocationForUpdate.getLocationName().equalsIgnoreCase(newLocation.getLocationName()))
		{
			LocationForUpdate.setLocationDescription(newLocation.getLocationDescription());
		} else
		{
			throw new Exception("Location with same Name already exists");
		}

		return locationRepo.save(LocationForUpdate);

	}

	public UsageLocation findSingleLocation(Long id)
	{
		Optional<UsageLocation> Locations = locationRepo.findById(id);
		return Locations.get();
	}

	public void deleteLocation(Long id) throws Exception
	{
		if (!checkBeforeDeleteService.isLocationUsed(id))
			locationRepo.softDeleteById(id);
		else
			throw new Exception("Location in use. Cannot delete.");

	}

	public List<IdNameProjections> findIdAndNames()
	{
		return locationRepo.findIdAndNames();
	}

	public Page<UsageLocation> findFilteredLocationsWithTA(FilterDataList filterDataList, Pageable pageable)
	{
		Specification<UsageLocation> spec = LocationSpecifications.getSpecification(filterDataList);
		if (spec != null)
			return locationRepo.findAll(spec, pageable);
		else
			return locationRepo.findAll(pageable);
	}

	public List<String> getTypeAheadForGlobalSearch(String str)
	{
		return locationRepo.getNamesForTypeAhead(str);
	}

}
