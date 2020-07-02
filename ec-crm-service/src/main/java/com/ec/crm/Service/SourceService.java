package com.ec.crm.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Source;
import com.ec.crm.Repository.SourceRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SourceService {
	@Autowired
	SourceRepo sRepo;
	public Page<Source> fetchAll(Pageable pageable) 
	{
		return sRepo.findAll(pageable);
	}
	
	public Source createSource(Source sourceData) throws Exception {
		if(!sRepo.existsBySourceName(sourceData.getSourceName()))
		{
			sRepo.save(sourceData);
			return sourceData;
		}else
		{
			throw new Exception("Source already exists!");
		}
	}
	
	public Source findSingleSource(long id) throws Exception 
	{
		Optional<Source> source = sRepo.findById(id);
		if(source.isPresent())
			return source.get();
		else
			throw new Exception("source ID not found");
	}
	
	public Source updateSource(Long id, Source source) throws Exception 
	{
		Optional<Source> SourceForUpdateOpt = sRepo.findById(id);
		Source SourceForUpdate = SourceForUpdateOpt.get();
		
		if(!SourceForUpdateOpt.isPresent())
			throw new Exception("Source not found with sourceid");
		
		if(!sRepo.existsBySourceName(source.getSourceName()) && !source.getSourceName().equalsIgnoreCase(SourceForUpdate.getSourceName()))
		{
			SourceForUpdate.setSourceName(source.getSourceName());
			SourceForUpdate.setSourceDescription(source.getSourceDescription());
			
		}
        else if(source.getSourceName().equalsIgnoreCase(SourceForUpdate.getSourceName()))
        {
  
        	SourceForUpdate.setSourceDescription(source.getSourceDescription());
        }
        else 
        {
        	throw new Exception("Source with same Name already exists");
        }
		return sRepo.save(SourceForUpdate);
		
	}
	
	public void deleteSource(Long id) throws Exception 
	{
		sRepo.softDeleteById(id);
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return sRepo.findIdAndNames();
	}
}
