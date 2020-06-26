package com.ec.crm.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.Source;
import com.ec.crm.Repository.SourceRepo;

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
		sRepo.save(sourceData);
		return sourceData;
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
		sRepo.save(source);
	    return source;
	}
	
	public void deleteSource(Long id) throws Exception 
	{
		sRepo.softDeleteById(id);
	}
}
