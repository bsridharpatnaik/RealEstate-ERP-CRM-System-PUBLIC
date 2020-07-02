package com.ec.crm.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Repository.SentimentRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SentimentService {
	@Autowired
	SentimentRepo sRepo;
	public Page<Sentiment> fetchAll(Pageable pageable) 
	{
		return sRepo.findAll(pageable);
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
        	throw new Exception("PropertyType with same Name already exists");
        }
		return sRepo.save(SentimentForUpdate);
	}
	
	public void deleteSentiment(Long id) throws Exception 
	{
		sRepo.softDeleteById(id);
	}
}
