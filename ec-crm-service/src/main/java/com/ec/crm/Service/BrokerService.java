package com.ec.crm.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.BrokerListWithTypeAheadData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Filters.BrokerSpecifications;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.SourceSpecifications;
import com.ec.crm.Model.Broker;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Source;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.ReusableClasses.IdNameProjections;

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
	public BrokerListWithTypeAheadData findFilteredBroker(FilterDataList brokerFilterDataList, Pageable pageable) 
	{
		BrokerListWithTypeAheadData tpData  = new BrokerListWithTypeAheadData();
		
		tpData.setBrokerDetails(getFilteredData(brokerFilterDataList,pageable));
		return tpData;
	}

	public Page<Broker> getFilteredData(FilterDataList brokerFilterDataList, Pageable pageable)
	{
		Specification<Broker> spec = fetchSpecification(brokerFilterDataList);
		if(spec!=null)
			return bRepo.findAll(spec, pageable);
		return bRepo.findAll(pageable);
	}
	
	private Specification<Broker> fetchSpecification(FilterDataList brokerFilterDataList) 
	{
		Specification<Broker> specification = null;
		for(FilterAttributeData attrData:brokerFilterDataList.getFilterData())
		{
			String attrName = attrData.getAttrName();
			List<String> attrValues = attrData.getAttrValue();
			
			Specification<Broker> internalSpecification = null;
			for(String attrValue : attrValues)
			{
				internalSpecification= internalSpecification==null?
						BrokerSpecifications.whereBrokernameContains(attrValue)
						:internalSpecification.or(BrokerSpecifications.whereBrokernameContains(attrValue));
			}
			specification= specification==null?internalSpecification:specification.and(internalSpecification);
		}
		return specification;
	}
	public Broker createBroker(Broker brokerData) throws Exception {
		if(!bRepo.existsByBrokerPhoneno(brokerData.getBrokerPhoneno()))
		{
			bRepo.save(brokerData);
			return brokerData;
		}
		else
		{
			throw new Exception("Broker already exists!");
		}
		
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
			
		}
        else if(broker.getBrokerPhoneno().equalsIgnoreCase(BrokerForUpdate.getBrokerPhoneno()))
        {
  
        	BrokerForUpdate.setBrokerPhoneno(broker.getBrokerPhoneno());
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
		bRepo.softDeleteById(id);
	}
	public List<IdNameProjections> findIdAndNames() 
	{
		return bRepo.findIdAndNames();
	}
}
