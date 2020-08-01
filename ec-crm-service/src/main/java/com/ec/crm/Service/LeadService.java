package com.ec.crm.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Data.LeadDetailInfo;
import com.ec.crm.Data.LeadListWithTypeAheadData;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Lead;
import com.ec.crm.Repository.AddressRepo;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NoteRepo;
import com.ec.crm.Repository.SentimentRepo;
import com.ec.crm.Repository.SourceRepo;
import com.ec.crm.ReusableClasses.CommonUtils;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class LeadService 
{
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	SentimentRepo siRepo;
	
	@Autowired
	BrokerRepo bRepo;
	
	@Autowired
	AddressRepo aRepo;
	
	@Autowired
	NoteRepo nRepo;
	
	@Autowired
	NoteService noteService;
	
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	SourceRepo sourceRepo;
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	PopulateDropdownService populateDropdownService;
	Long currentUserID;
	
	@PersistenceContext
    private EntityManager entityManager;;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
	CommonUtils utilObj = new CommonUtils();
		
	Logger log = LoggerFactory.getLogger(LeadService.class);

	@Transactional
	public Lead createLead(@Valid LeadCreateData payload) throws Exception 
	{
		log.info("Create Lead invoked with payload " + payload.toString());
		currentUserID = userDetailsService.getCurrentUser().getId();
		log.info("Fetched current user from common-service " + currentUserID.toString());
		Lead lead = new Lead();
		log.info("Formatting mobile number received in payload");
		formatMobileNo(payload);
		log.info("Validating Payload");
		validatePayload(payload);
		log.info("Payload Validated");
		log.info("Checking if record exists with mobile number");
		exitIfMobileNoExists(payload);
		log.info("Setting lead fields from payload");
		setLeadFields(lead,payload,"create");
		log.info("Saving new lead record to database");
		return lRepo.save(lead);
	}
	
	@Transactional
	public Lead updateLead(@Valid LeadCreateData payload,Long id) throws Exception 
	{
		log.info("Update Lead invoked with payload " + payload.toString());
		currentUserID = userDetailsService.getCurrentUser().getId();
		log.info("Fetched current user from common-service " + currentUserID.toString());
		Optional<Lead> leadOpt = lRepo.findById(id);
		
		if(!leadOpt.isPresent())
			throw new Exception("Lead with ID -"+id+" Not Found");
		
		
		Lead leadForUpdate = leadOpt.get();
		log.info("Formatting mobile number received in payload");
		formatMobileNo(payload);
		log.info("Validating payload");
		validatePayload(payload);
		log.info("Payload Validated");
		if(!leadForUpdate.getPrimaryMobile().equals(payload.getPrimaryMobile()))
			exitIfMobileNoExists(payload);
		log.info("Setting lead fields from payload");
		setLeadFields(leadForUpdate,payload,"update");
		log.info("Saving new lead record to database");
		return lRepo.save(leadForUpdate);
	}
	
	public Lead getSingleLead(Long id) throws Exception 
	{
		Optional<Lead> leadOpt = lRepo.findById(id);
		
		if(!leadOpt.isPresent())
			throw new Exception("Lead with ID -"+id+" Not Found");
		
		return leadOpt.get();
	}
	
	private void formatMobileNo(LeadCreateData payload) 
	 {
		log.info("Invoked formatMobileNo");
       if (!(payload.getPrimaryMobile() == null) && !payload.getPrimaryMobile().equals(""))
           payload.setPrimaryMobile(utilObj.normalizePhoneNumber(payload.getPrimaryMobile()));
       if(payload.getSecondaryMobile()!= null && payload.getSecondaryMobile()!="")
       	payload.setSecondaryMobile(utilObj.normalizePhoneNumber(payload.getSecondaryMobile()));
   }
	
	
	
	 private void setLeadFields(Lead lead, @Valid LeadCreateData payload,String type) 
		{
		 	log.info("Invoked setLeadFields");
			lead.setCustomerName(payload.getCustomerName());
			lead.setPrimaryMobile(payload.getPrimaryMobile());
			lead.setSecondaryMobile(payload.getSecondaryMobile());
			lead.setDateOfBirth(payload.getDateOfBirth());
			lead.setEmailId(payload.getEmailId());
			lead.setOccupation(payload.getOccupation());
			lead.setPurpose(payload.getPurpose());
			lead.setPropertyType(PropertyTypeEnum.valueOf(payload.getPropertyType()));
			lead.setSentiment(payload.getSentimentId()==null?null:siRepo.findById(payload.getSentimentId()).get());
			lead.setSource(payload.getSourceId()==null?null:sourceRepo.findById(payload.getSourceId()).get());
			lead.setBroker(payload.getBrokerId()==null?null:bRepo.findById(payload.getBrokerId()).get());
			lead.setAsigneeId(lead.getAsigneeId()==null?currentUserID:userDetailsService.getUserFromId(payload.getAssigneeId()).getId());
			
			if(type.equalsIgnoreCase("create"))
			{
				lead.setStatus(LeadStatusEnum.New_Lead);
				lead.setCreatorId(currentUserID);
				lead.setAddress(setAddress(payload, new Address()));
			}
			else if(type.equalsIgnoreCase("update"))
				lead.setAddress(setAddress(payload,lead.getAddress()));
		}
	 
	 @Transactional
		private Address setAddress(@Valid LeadCreateData payload,Address address) 
		{
			log.info("Invoked setAddress");
			address.setAddr_line1(payload.getAddressLine1());
			address.setAddr_line2(payload.getAddressLine2());
			address.setCity(payload.getCity());
			address.setPincode(payload.getPincode());
			return aRepo.save(address);
		}
		
	public Page<Lead> fetchAll(Pageable pageable) 
	{
		return lRepo.findAll(pageable);
	}
	
	public LeadListWithTypeAheadData findFilteredList(FilterDataList leadFilterDataList, Pageable pageable) throws ParseException 
	{
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadListWithTypeAheadData leadListWithTypeAheadData = new LeadListWithTypeAheadData();
		
		log.info("Fetching filteration based on filter data received");
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
		
		log.info("Fetching records based on specification");
		if(spec!=null)
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(spec, pageable));
		else 		
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(pageable));
		log.info("Setting dropdown data");
		leadListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting tyoeahead data");
		leadListWithTypeAheadData.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
		return leadListWithTypeAheadData;
	}
	
	private List<String> fetchTypeAheadForLeadGlobalSearch() 
	{
		log.info("Invoked fetchTypeAheadForLeadGlobalSearch");
		List<String> typeAhead = new ArrayList<String>();
		typeAhead.addAll(lRepo.getLeadNames());
		typeAhead.addAll(lRepo.getLeadMobileNos());
		return typeAhead;
	}

	public LeadDetailInfo findSingleLeadDetailInfo(long id) throws Exception 
	{
		LeadDetailInfo leadDetails = new LeadDetailInfo();
		leadDetails.setAllNotes(noteService.getAllNotesForLead(id));
		return leadDetails;
	}
	
 
	public void deleteLead(Long id) {
		// TODO Auto-generated method stub
		log.info("Invoked deleteLead for ID - "+id);
		lRepo.softDeleteById(id);
	}

	public List<Lead> history(Long id)
	{
		AuditReader reader = AuditReaderFactory.get(entityManager);
		AuditQuery q = reader.createQuery().forRevisionsOfEntity(Lead.class, true, true);
		q.add(AuditEntity.id().eq(id));
		List<Lead> audit = q.getResultList();
		return audit;
	}
	
	private void validatePayload(LeadCreateData payload) throws Exception 
	 {
		log.info("Invoked validatePayload");
	  	if(!validateRequiredFields(payload).equals(""))
	  		throw new Exception("Required fields missing - " + validateRequiredFields(payload));
			
	  	if(!ReusableMethods.isValidMobileNumber(payload.getPrimaryMobile()))
	  		throw new Exception("Please enter valid mobile number.");
			
	  	if(payload.getSecondaryMobile()!=null && payload.getSecondaryMobile()!="")
	  		if(!ReusableMethods.isValidMobileNumber(payload.getSecondaryMobile()))
	  			throw new Exception("Please enter valid mobile number.");
	  	
	  	if(payload.getEmailId()!=null && payload.getEmailId()!="")
	  		if(!ReusableMethods.isValidEmail(payload.getEmailId()))
	  					throw new Exception("Please enter valid EmailId.");
	  	
	  	if(payload.getPincode()!=null && payload.getPincode()!="")
	  		if(!payload.getPincode().matches( "\\d{6}"))
	  			throw new Exception("Enter a valid pin code (6 Digits numeric)");
	 }
	
	private void exitIfMobileNoExists(LeadCreateData payload) throws Exception
	{
		log.info("Invoked exitIfMobileNoExists");
		if(lRepo.findCountByPMobileNo(payload.getPrimaryMobile())>0)
			throw new Exception("Contact already exists by Primary Mobile Number.");
	}
	
	 private String validateRequiredFields(LeadCreateData payload) 
	 {
		 log.info("Invoked validateRequiredFields");
		String message = "";
		if(payload.getPrimaryMobile()==null || payload.getPrimaryMobile().equals(""))
			message = message==""?"Primary Mobile No.":message+",Primary Mobile No.";
		
		if(payload.getCustomerName()==null || payload.getCustomerName().equals(""))
			message = message==""?"Customer Name":message+", Customer Name";
	
		return message;
	 }
	
}
