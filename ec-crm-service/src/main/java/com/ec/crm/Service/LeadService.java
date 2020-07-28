package com.ec.crm.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadStatusEnum;
import com.ec.crm.Model.PropertyTypeEnum;
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
		
	

	@Transactional
	public Lead createLead(@Valid LeadCreateData payload) throws Exception 
	{
		currentUserID = userDetailsService.getCurrentUser().getId();
		Lead lead = new Lead();
		formatMobileNo(payload);
		validatePayload(payload);
		exitIfMobileNoExists(payload);
		setLeadFields(lead,payload,"create");
		return lRepo.save(lead);
	}
	
	@Transactional
	public Lead updateLead(@Valid LeadCreateData payload,Long id) throws Exception 
	{
		currentUserID = userDetailsService.getCurrentUser().getId();
		Optional<Lead> leadOpt = lRepo.findById(id);
		
		if(!leadOpt.isPresent())
			throw new Exception("Lead with ID -"+id+" Not Found");
		
		Lead leadForUpdate = leadOpt.get();
		formatMobileNo(payload);
		validatePayload(payload);
		
		if(!leadForUpdate.getPrimaryMobile().equals(payload.getPrimaryMobile()))
			exitIfMobileNoExists(payload);
		
		setLeadFields(leadForUpdate,payload,"update");
		return lRepo.save(leadForUpdate);
	}
	
	private void formatMobileNo(LeadCreateData payload) 
	 {
   	//System.out.println(payload.getContactPersonMobileNo());
       if (!(payload.getPrimaryMobile() == null) && !payload.getPrimaryMobile().equals(""))
           payload.setPrimaryMobile(utilObj.normalizePhoneNumber(payload.getPrimaryMobile()));
       if(payload.getSecondaryMobile()!= null && payload.getSecondaryMobile()!="")
       	payload.setSecondaryMobile(utilObj.normalizePhoneNumber(payload.getSecondaryMobile()));
   }
	
	
	
	 private void setLeadFields(Lead lead, @Valid LeadCreateData payload,String type) 
		{
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
	 
		private Address setAddress(@Valid LeadCreateData payload,Address address) 
		{
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
		LeadListWithTypeAheadData leadListWithTypeAheadData = new LeadListWithTypeAheadData();
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
		
		if(spec!=null)
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(spec, pageable));
		else 		
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(pageable));

		leadListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		leadListWithTypeAheadData.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
		return leadListWithTypeAheadData;
	}
	
	private List<String> fetchTypeAheadForLeadGlobalSearch() 
	{
		List<String> typeAhead = new ArrayList<String>();
		typeAhead.addAll(lRepo.getLeadNames());
		typeAhead.addAll(lRepo.getLeadMobileNos());
		return typeAhead;
	}

	public LeadDetailInfo findSingleLeadDetailInfo(long id) throws Exception 
	{/*
		// TODO Auto-generated method stub
		LeadDetailInfo l=new LeadDetailInfo();
		Optional<Lead> lead = lRepo.findById(id);
		if(lead.isPresent())
		{
			l.setLeadDetails(lead.get());
			List<Note> pinnednotes=nRepo.getpinnednotes(id);
			List<Note> unpinnednotes=nRepo.getunpinnednotes(id);
			List<LeadStatus> leadstatus=lsRepo.checklatest(id);
			if(!leadstatus.isEmpty()) {
				/*set latest status 
				Long statusid=leadstatus.get(0).getStatusId();
				Optional<Status> Ostatus=stRepo.findById(statusid);
				Status status=Ostatus.get();
				l.setStatusInfo(status);
				
				//set historical status
				List<Status> s=new ArrayList<Status>();
				for (int i = 0; i < leadstatus.size(); i++) {
					Long sid = leadstatus.get(i).getStatusId();
					Optional<Status> Ostatusadd=stRepo.findById(sid);
					s.add(Ostatusadd.get());
				}
				l.setHistoricalStatus(null);
			}
			
			l.setPinnedNotes(pinnednotes);
			l.setUnpinnedNotes(unpinnednotes);
			
			return null;
		}
		else {
			throw new Exception("Lead ID not found");
		}
			*/
		return null;
	}
	
 
	public void deleteLead(Long id) {
		// TODO Auto-generated method stub
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
		if(lRepo.findCountByPMobileNo(payload.getPrimaryMobile())>0)
			throw new Exception("Contact already exists by Primary Mobile Number.");
	}
	
	 private String validateRequiredFields(LeadCreateData payload) 
	 {
		String message = "";
		if(payload.getPrimaryMobile()==null || payload.getPrimaryMobile().equals(""))
			message = message==""?"Primary Mobile No.":message+",Primary Mobile No.";
		
		if(payload.getCustomerName()==null || payload.getCustomerName().equals(""))
			message = message==""?"Customer Name":message+", Customer Name";
	
		return message;
	 }
	
}
