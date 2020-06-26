package com.ec.crm.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.Broker;
import com.ec.crm.Repository.BrokerRepo;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class BrokerService {
	@Autowired
	BrokerRepo bRepo;
	public Page<Broker> fetchAll(Pageable pageable) 
	{
		return bRepo.findAll(pageable);
	}
	
	public Broker createBroker(Broker brokerData) throws Exception {
		bRepo.save(brokerData);
		return brokerData;
	}
	
	public Broker findSingleBroker(long id) throws Exception 
	{
		Optional<Broker> broker = bRepo.findById(id);
		if(broker.isPresent())
			return broker.get();
		else
			throw new Exception("broker ID not found");
	}
	
	public Broker updateBroker(Long id, Broker broker) throws Exception 
	{
		bRepo.save(broker);
	    return broker;
	}
	
	public void deleteBroker(Long id) throws Exception 
	{
		bRepo.softDeleteById(id);
	}
}
