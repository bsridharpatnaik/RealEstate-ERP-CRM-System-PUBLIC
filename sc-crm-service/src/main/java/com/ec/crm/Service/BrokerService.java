package com.ec.crm.Service;

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
import com.ec.crm.ReusableClasses.IdNameProjections;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class BrokerService
{
	@Autowired
	BrokerRepo bRepo;

	@Autowired
	CheckBeforeDeleteService checkBeforeDeleteService;

	public Page<Broker> fetchAll(Pageable pageable)
	{
		return bRepo.findAll(pageable);
	}

	public BrokerListWithTypeAheadData findFilteredBroker(FilterDataList brokerFilterDataList, Pageable pageable)
	{
		BrokerListWithTypeAheadData brokerListWithTypeAheadData = new BrokerListWithTypeAheadData();
		Specification<Broker> spec = BrokerSpecifications.fetchSpecification(brokerFilterDataList);
		brokerListWithTypeAheadData
				.setBrokerDetails(spec != null ? bRepo.findAll(spec, pageable) : bRepo.findAll(pageable));
		List<String> typeAheadData = bRepo.getUniqueNames();
		typeAheadData.addAll(bRepo.getUniqueNumbers());
		brokerListWithTypeAheadData.setTypeAheadData(typeAheadData);
		return brokerListWithTypeAheadData;
	}

	public Broker createBroker(Broker brokerData) throws Exception
	{
		validatePayload(brokerData);
		brokerData.setBrokerPhoneno(ReusableMethods.normalizePhoneNumber(brokerData.getBrokerPhoneno()));
		if (!bRepo.existsByBrokerPhoneno(brokerData.getBrokerPhoneno()))
			return bRepo.save(brokerData);
		else
			throw new Exception("Broker already exists with same mobile number!");
	}

	private void validatePayload(Broker brokerData) throws Exception
	{
		if (brokerData.getBrokerPhoneno() == null || brokerData.getBrokerPhoneno() == "")
			throw new Exception("Please enter broker mobile number");
		if (!ReusableMethods.isValidMobileNumber(brokerData.getBrokerPhoneno()))
			throw new Exception("Please enter valid Mobile Number");
	}

	public Broker findSingleBroker(long id) throws Exception
	{
		Optional<Broker> broker = bRepo.findById(id);
		if (broker.isPresent())
			return broker.get();
		else
			throw new Exception("broker ID not found");
	}

	public Broker updateBroker(Long id, Broker broker) throws Exception
	{
		Optional<Broker> BrokerForUpdateOpt = bRepo.findById(id);
		Broker BrokerForUpdate = BrokerForUpdateOpt.get();

		if (!BrokerForUpdateOpt.isPresent())
			throw new Exception("Broker not found with brokerid");

		if (!bRepo.existsByBrokerPhoneno(broker.getBrokerPhoneno())
				&& !broker.getBrokerPhoneno().equalsIgnoreCase(BrokerForUpdate.getBrokerPhoneno()))
		{
			BrokerForUpdate.setBrokerName(broker.getBrokerName());
			BrokerForUpdate.setBrokerAddress(broker.getBrokerAddress());
			BrokerForUpdate.setBrokerPhoneno(broker.getBrokerPhoneno());
		} else if (broker.getBrokerPhoneno().equalsIgnoreCase(BrokerForUpdate.getBrokerPhoneno()))
		{
			BrokerForUpdate.setBrokerName(broker.getBrokerName());
			BrokerForUpdate.setBrokerAddress(broker.getBrokerAddress());
		} else
		{
			throw new Exception("Broker with same Phone already exists");
		}
		return bRepo.save(BrokerForUpdate);
	}

	public void deleteBroker(Long id) throws Exception
	{
		if (checkBeforeDeleteService.isBrokerUsed(id))
			throw new Exception("Broker already in use. Cannot be deleted.");
		else
			bRepo.softDeleteById(id);
	}

	public List<IdNameProjections> findIdAndNames()
	{
		return bRepo.findIdAndNames();
	}
}
