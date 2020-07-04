package com.ec.crm.Service;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Broker;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Model.Source;
import com.ec.crm.Repository.AddressRepo;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.PropertyTypeRepo;
import com.ec.crm.Repository.SentimentRepo;
import com.ec.crm.Repository.SourceRepo;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class LeadService {
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	SourceRepo soRepo;
	
	@Autowired
	SentimentRepo siRepo;
	
	@Autowired
	BrokerRepo bRepo;
	
	@Autowired
	PropertyTypeRepo pRepo;
	
	@Autowired
	AddressRepo aRepo;
	
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
	public Page<Lead> fetchAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return lRepo.findAll(pageable);
	}

	public Lead findSingleLead(long id) throws Exception {
		// TODO Auto-generated method stub
		Optional<Lead> lead = lRepo.findById(id);
		if(lead.isPresent())
			return lead.get();
		else
			throw new Exception("Lead ID not found");
	}

	public Lead createLead(@Valid LeadCreateData payload) throws Exception {
		// TODO Auto-generated method stub
		Optional<Sentiment> sentimentOpt = siRepo.findById(payload.getSentimentId());
		if(! sentimentOpt.isPresent())
			throw new Exception("Sentiment not found");
		
		Optional<Source> sourceOpt = soRepo.findById(payload.getSourceId());
		if(! sourceOpt.isPresent())
			throw new Exception("Source not found");
		
		Optional<Broker> brokerOpt = bRepo.findById(payload.getBrokerId());
		if(! sentimentOpt.isPresent())
			throw new Exception("Broker not found");
		
		Optional<PropertyType> propertyTypeOpt = pRepo.findById(payload.getPropertyTypeId());
		if(! sentimentOpt.isPresent())
			throw new Exception("PropertyType not found");
		
		Optional<Address> addressOpt = aRepo.findById(payload.getAddressId());
		if(! addressOpt.isPresent())
			throw new Exception("Address not found");
		
		Long userId;
		UserReturnData userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/me")
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData.class)
		    	.block();
		userId= userDetails.getId();

		
		Lead lead=new Lead();
		lead.setCustomerName(payload.getCustomerName());
		lead.setPrimaryMobile(payload.getPrimaryMobile());
		lead.setSecondaryMobile(payload.getSecondaryMobile());
		lead.setDateOfBirth(payload.getDateOfBirth());
		lead.setEmailId(payload.getEmailId());
		lead.setOccupation(payload.getOccupation());
		lead.setPurposeId(payload.getPurposeId());
		lead.setPropertyType(propertyTypeOpt.get());
		lead.setSentiment(sentimentOpt.get());
		lead.setSource(sourceOpt.get());
		lead.setBroker(brokerOpt.get());
		lead.setAddress(addressOpt.get());
		lead.setUserId(userId);
		lRepo.save(lead);
		return lead;
	}

	public void deleteLead(Long id) {
		// TODO Auto-generated method stub
		lRepo.softDeleteById(id);
	}

	public Lead updateLead(Long id, LeadCreateData payload) throws Exception {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		Optional<Lead> leadOpt = lRepo.findById(id);
		if(! leadOpt.isPresent())
			throw new Exception("Lead not found");
		
		Optional<Sentiment> sentimentOpt = siRepo.findById(payload.getSentimentId());
		if(! sentimentOpt.isPresent())
			throw new Exception("Sentiment not found");
		
		Optional<Source> sourceOpt = soRepo.findById(payload.getSourceId());
		if(! sourceOpt.isPresent())
			throw new Exception("Source not found");
		
		Optional<Broker> brokerOpt = bRepo.findById(payload.getBrokerId());
		if(! sentimentOpt.isPresent())
			throw new Exception("Broker not found");
		
		Optional<PropertyType> propertyTypeOpt = pRepo.findById(payload.getPropertyTypeId());
		if(! sentimentOpt.isPresent())
			throw new Exception("PropertyType not found");
		
		Optional<Address> addressOpt = aRepo.findById(payload.getAddressId());
		if(! addressOpt.isPresent())
			throw new Exception("Address not found");
		
		Long userId;
		UserReturnData userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/me")
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData.class)
		    	.block();
		userId= userDetails.getId();

		
		Lead lead=leadOpt.get();
		lead.setCustomerName(payload.getCustomerName());
		lead.setPrimaryMobile(payload.getPrimaryMobile());
		lead.setSecondaryMobile(payload.getSecondaryMobile());
		lead.setDateOfBirth(payload.getDateOfBirth());
		lead.setEmailId(payload.getEmailId());
		lead.setOccupation(payload.getOccupation());
		lead.setPurposeId(payload.getPurposeId());
		lead.setPropertyType(propertyTypeOpt.get());
		lead.setSentiment(sentimentOpt.get());
		lead.setSource(sourceOpt.get());
		lead.setBroker(brokerOpt.get());
		lead.setAddress(addressOpt.get());
		lead.setUserId(userId);
		lRepo.save(lead);
		return lead;
	}
		
	
	
	
}
