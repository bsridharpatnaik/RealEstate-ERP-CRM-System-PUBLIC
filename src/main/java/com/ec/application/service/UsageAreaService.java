package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.AllUsageAreasWithNamesData;
import com.ec.application.model.UsageArea;
import com.ec.application.repository.UsageAreaRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.UsageAreaSpecifications;


@Service
public class UsageAreaService 
{

	@Autowired
	UsageAreaRepo usageAreaRepo;
	
	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;
	
	public Page<UsageArea> findAll(Pageable pageable)
	{
		return usageAreaRepo.findAll(pageable);
    }
	
	public UsageArea createUsageArea(UsageArea payload) throws Exception 
	{
		if(!usageAreaRepo.existsByUsageAreaName(payload.getUsageAreaName()))
		{
			usageAreaRepo.save(payload);
			return payload;
		}
		else
		{
			throw new Exception("UsageArea already exists!");
		}
    }

	public UsageArea updateUsageArea(Long id, UsageArea payload) throws Exception 
	{
		Optional<UsageArea> UsageAreaForUpdateOpt = usageAreaRepo.findById(id);
        UsageArea UsageAreaForUpdate = UsageAreaForUpdateOpt.get();
        
		UsageArea newUsageArea = new UsageArea();
        newUsageArea = payload;
        if(!usageAreaRepo.existsByUsageAreaName(newUsageArea.getUsageAreaName()) && 
        		!newUsageArea.getUsageAreaName().equalsIgnoreCase(UsageAreaForUpdate.getUsageAreaName()))
        {		
        	UsageAreaForUpdate.setUsageAreaName(newUsageArea.getUsageAreaName());
            UsageAreaForUpdate.setUsageAreaDescription(newUsageArea.getUsageAreaDescription());
        }
        else if(newUsageArea.getUsageAreaName().equalsIgnoreCase(UsageAreaForUpdate.getUsageAreaName()))
        {
        	UsageAreaForUpdate.setUsageAreaDescription(newUsageArea.getUsageAreaDescription());
        }
        else 
        {
        	throw new Exception("UsageArea with same Name already exists");
        }
        	
        return usageAreaRepo.save(UsageAreaForUpdate);
        
    }

	public UsageArea findSingleUsageArea(Long id) 
	{
		Optional<UsageArea> UsageAreas = usageAreaRepo.findById(id);
		return UsageAreas.get();
	}
	public void deleteUsageArea(Long id) throws Exception 
	{
			if(!checkBeforeDeleteService.isUsageAreaUsed(id))
				usageAreaRepo.softDeleteById(id);
			else
				throw new Exception("Cannot delete usageArea. UsageArea already in use.");
	}

	public List<IdNameProjections> findIdAndNames() 
	{
		// TODO Auto-generated method stub
		return usageAreaRepo.findIdAndNames();
	}

	public AllUsageAreasWithNamesData findFilteredUsageAreasWithTA(FilterDataList filterDataList, Pageable pageable) 
	{
		AllUsageAreasWithNamesData allUsageAreasWithNamesData = new AllUsageAreasWithNamesData();
		Specification<UsageArea> spec = UsageAreaSpecifications.getSpecification(filterDataList);
		
		if(spec!=null) allUsageAreasWithNamesData.setUsageAreas(usageAreaRepo.findAll(spec, pageable));
		else allUsageAreasWithNamesData.setUsageAreas(usageAreaRepo.findAll(pageable));

		allUsageAreasWithNamesData.setNames(ReusableMethods.removeNullsFromStringList(usageAreaRepo.getNames()));
		return allUsageAreasWithNamesData;
		
	}
}
