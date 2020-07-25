package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.BrokerListWithTypeAheadData;
import com.ec.crm.Filters.BrokerSpecifications;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.Broker;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class BrokerService 
{
	@Autowired
	BrokerRepo bRepo;
	
	
	@Autowired
	LeadRepo lRepo;
	
	
	public Page<Broker> fetchAll(Pageable pageable) 
	{
		return bRepo.findAll(pageable);
	}

	public BrokerListWithTypeAheadData findFilteredBroker(FilterDataList brokerFilterDataList, Pageable pageable)
	{
		BrokerListWithTypeAheadData brokerListWithTypeAheadData = new BrokerListWithTypeAheadData();
		Specification<Broker> spec = BrokerSpecifications.fetchSpecification(brokerFilterDataList);
		brokerListWithTypeAheadData.setBrokerDetails(spec!=null?bRepo.findAll(spec, pageable):bRepo.findAll(pageable));
		List<String> typeAheadData = bRepo.getUniqueNames();
		typeAheadData.addAll(bRepo.getUniqueNumbers());
		brokerListWithTypeAheadData.setTypeAheadData(typeAheadData);
		return brokerListWithTypeAheadData;
	}
	
	public Broker createBroker(Broker brokerData) throws Exception 
	{
		if(!bRepo.existsByBrokerPhoneno(brokerData.getBrokerPhoneno()))
			return bRepo.save(brokerData);
		else
			throw new Exception("Broker already exists!");
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
		Optional<Broker> BrokerForUpdateOpt = bRepo.findById(id);
		Broker BrokerForUpdate = BrokerForUpdateOpt.get();
		
		if(!BrokerForUpdateOpt.isPresent())
			throw new Exception("Broker not found with brokerid");
		
		if(!bRepo.existsByBrokerPhoneno(broker.getBrokerPhoneno()) && !broker.getBrokerPhoneno().equalsIgnoreCase(BrokerForUpdate.getBrokerPhoneno()))
		{
			BrokerForUpdate.setBrokerName(broker.getBrokerName());
			BrokerForUpdate.setBrokerAddress(broker.getBrokerAddress());
			BrokerForUpdate.setBrokerPhoneno(broker.getBrokerPhoneno());
		}
        else if(broker.getBrokerPhoneno().equalsIgnoreCase(BrokerForUpdate.getBrokerPhoneno()))
        {
        	BrokerForUpdate.setBrokerName(broker.getBrokerName());
			BrokerForUpdate.setBrokerAddress(broker.getBrokerAddress());
        }
        else 
        {
        	throw new Exception("Broker with same Phone already exists");
        }
		return bRepo.save(BrokerForUpdate);
	}
	
	public void deleteBroker(Long id) throws Exception 
	{
		int used=lRepo.checkbrokerusedinlead(id);
//		System.out.println(used);
		if(used>0) {
			throw new Exception("Broker used in leads");
		}else {
			bRepo.softDeleteById(id);
		}
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return bRepo.findIdAndNames();
	}
}
