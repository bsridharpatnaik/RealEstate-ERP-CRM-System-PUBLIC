package com.ec.crm.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.SentimentListWithTypeAheadData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.SentimentSpecifications;
import com.ec.crm.Filters.SourceSpecifications;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Model.Source;
import com.ec.crm.Repository.SentimentRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SentimentService 
{
	@Autowired
	SentimentRepo sRepo;
	
	public Page<Sentiment> fetchAll(Pageable pageable) 
	{
		return sRepo.findAll(pageable);
	}
	
	public SentimentListWithTypeAheadData findFilteredSource(FilterDataList sentimentFilterDataList, Pageable pageable) 
	{
		SentimentListWithTypeAheadData tpData  = new SentimentListWithTypeAheadData();
		tpData.setSentimentDetails(getFilteredData(sentimentFilterDataList,pageable));
		tpData.setSentimentTypeAhead(sRepo.findNamesList());
		return tpData;
	}

	public Page<Sentiment> getFilteredData(FilterDataList sentimentFilterDataList, Pageable pageable)
	{
		Specification<Sentiment> spec = fetchSpecification(sentimentFilterDataList);
		if(spec!=null)
			return sRepo.findAll(spec, pageable);
		return sRepo.findAll(pageable);
	}
	
	private Specification<Sentiment> fetchSpecification(FilterDataList sourceFilterDataList) 
	{
		Specification<Sentiment> specification = null;
		for(FilterAttributeData attrData:sourceFilterDataList.getFilterData())
		{
			String attrName = attrData.getAttrName();
			List<String> attrValues = attrData.getAttrValue();
			
			Specification<Sentiment> internalSpecification = null;
			for(String attrValue : attrValues)
			{
				internalSpecification= internalSpecification==null?
						SentimentSpecifications.whereSentimentnameContains(attrValue)
						:internalSpecification.or(SentimentSpecifications.whereSentimentnameContains(attrValue));
			}
			specification= specification==null?internalSpecification:specification.and(internalSpecification);
		}
		return specification;
	}
	public Sentiment createSentiment(Sentiment sentimentData) throws Exception {
		if(!sRepo.existsByName(sentimentData.getName()))
		{
			sRepo.save(sentimentData);
			return sentimentData;
		}
		else
		{
			throw new Exception("Sentiment already exists!");
		}
	}
	
	public Sentiment findSingleSentiment(long id) throws Exception 
	{
		Optional<Sentiment> sentiment = sRepo.findById(id);
		if(sentiment.isPresent())
			return sentiment.get();
		else
			throw new Exception("sentiment ID not found");
	}
	
	public Sentiment updateSentiment(Long id, Sentiment sentiment) throws Exception 
	{
		Optional<Sentiment> SentimentForUpdateOpt = sRepo.findById(id);
		Sentiment SentimentForUpdate = SentimentForUpdateOpt.get();
		
		if(!SentimentForUpdateOpt.isPresent())
			throw new Exception("Sentiment not found with sentimentid");
		
		if(!sRepo.existsByName(sentiment.getName()) && !sentiment.getName().equalsIgnoreCase(SentimentForUpdate.getName()))
		{
			SentimentForUpdate.setName(sentiment.getName());
			SentimentForUpdate.setDescription(sentiment.getDescription());
			
		}
        else if(sentiment.getName().equalsIgnoreCase(SentimentForUpdate.getName()))
        {
  
        	SentimentForUpdate.setDescription(sentiment.getDescription());
        }
        else 
        {
        	throw new Exception("Sentiment with same name already exists.");
        }
		return sRepo.save(SentimentForUpdate);
	}
	
	public void deleteSentiment(Long id) throws Exception 
	{
		//Add logic to delete only if not used by any lead
		sRepo.softDeleteById(id);
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return sRepo.findIdAndNames();
	}
}
