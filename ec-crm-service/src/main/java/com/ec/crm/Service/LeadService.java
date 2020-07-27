package com.ec.crm.Service;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
import com.ec.crm.Data.ReturnCreatedLead;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Broker;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadStatus;
import com.ec.crm.Model.PropertyTypeEnum;
import com.ec.crm.Model.Sentiment;
import com.ec.crm.Model.StatusEnum;
import com.ec.crm.Repository.AddressRepo;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.LeadStatusRepo;
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
	LeadStatusRepo lsRepo;	
	
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
	
	Long currentUserID;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
	CommonUtils utilObj = new CommonUtils();
		


	public ReturnCreatedLead createLead(@Valid LeadCreateData payload) throws Exception 
	{
		currentUserID = userDetailsService.getCurrentUser().getId();
		Lead lead = new Lead();
		formatMobileNo(payload);
		validatePayload(payload);
		setLeadFields(lead,payload,"create");
		lead = lRepo.save(lead);
		StatusEnum updatedStatus  = setLeadStatus(lead,payload.getStatus(),"create");
		return new ReturnCreatedLead(updatedStatus,lead);
	}
	
	private void formatMobileNo(LeadCreateData payload) 
	 {
   	//System.out.println(payload.getContactPersonMobileNo());
       if (!(payload.getPrimaryMobile() == null) && !payload.getPrimaryMobile().equals(""))
           payload.setPrimaryMobile(utilObj.normalizePhoneNumber(payload.getPrimaryMobile()));
       if(!payload.getSecondaryMobile().equals(""))
       	payload.setSecondaryMobile(utilObj.normalizePhoneNumber(payload.getSecondaryMobile()));
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
		
		if(payload.getAssigneeId()==null)
			message = message==""?"Assignee ":message+", Assignee ";
		
		if(payload.getStatus()==null)
			message = message==""?"Status ":message+", Status ";
		return message;
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
			lead.setSentiment(siRepo.findById(payload.getSentimentId()).get());
			lead.setSource(sourceRepo.findById(payload.getSourceId()).get());
			lead.setBroker(bRepo.findById(payload.getBrokerId()).get());
			
			if(type.equalsIgnoreCase("create"))
			{
				System.out.println("Current User - "+currentUserID);
				lead.setAsigneeId(currentUserID);
				lead.setCreatorId(currentUserID);
				lead.setAddress(setAddress(payload, new Address()));
			}
			else if(type.equalsIgnoreCase("update"))
			{
				lead.setAsigneeId(userDetailsService.getUserFromId(payload.getAssigneeId()).getId());
				lead.setAddress(setAddress(payload,lead.getAddress()));
			}
			
		}

		
		private StatusEnum setLeadStatus(Lead lead, String string2,String string) 
		{
			LeadStatus lstatus=null;
			switch(string)
			{
				
				case "create":
					lstatus = new LeadStatus(lead.getLeadId(),string2,currentUserID,currentUserID);
					lsRepo.save(lstatus);
					break;
				case "udpate":
					lstatus= lsRepo.FindLeadStatusByLeadID(lead.getLeadId()).get(0);
					if(!lstatus.getStatus().equals(lstatus))
					{
						lstatus = new LeadStatus(lead.getLeadId(),string2,currentUserID,currentUserID);
						lsRepo.save(lstatus);
					}
					break;
			}
			return lstatus.getStatus();
		}

		private Address setAddress(@Valid LeadCreateData payload,Address address) 
		{
			address.setAddr_line1(payload.getAddressLine1());
			address.setAddr_line2(payload.getAddressLine2());
			address.setCity(payload.getCity());
			address.setPincode(payload.getPincode());
			return aRepo.save(address);
		}
		
	public Page<Lead> fetchAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return lRepo.findAll(pageable);
	}
	
	public LeadListWithTypeAheadData findFilteredList(FilterDataList leadFilterDataList, Pageable pageable) 
	{
		LeadListWithTypeAheadData leadListWithTypeAheadData = new LeadListWithTypeAheadData();
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);
		
		if(spec!=null)
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(spec, pageable));
		else 		
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(pageable));

		return leadListWithTypeAheadData;
	}
	public Lead findSingleLead(long id) throws Exception {
		// TODO Auto-generated method stub
		Optional<Lead> lead = lRepo.findById(id);
		if(lead.isPresent())
			return lead.get();
		else
			throw new Exception("Lead ID not found");
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

	public Lead updateLead(Long id, LeadCreateData payload) throws Exception {
		// TODO Auto-generated method stub
		Optional<Lead> leadOpt = lRepo.findById(id);
		if(! leadOpt.isPresent())
			throw new Exception("Lead not found");
		
		Optional<Sentiment> sentimentOpt = siRepo.findById(payload.getSentimentId());
		if(! sentimentOpt.isPresent())
			throw new Exception("Sentiment not found");
		
		/*
		 * Optional<Source> sourceOpt = soRepo.findById(payload.getSourceId()); if(!
		 * sourceOpt.isPresent()) throw new Exception("Source not found");
		 */
		
		Optional<Broker> brokerOpt = bRepo.findById(payload.getBrokerId());
		if(! brokerOpt.isPresent())
			throw new Exception("Broker not found");
		
		/*
		 * Optional<PropertyTypeEnum> propertyTypeOpt =
		 * pRepo.findById(payload.getPropertyTypeId()); if(! sentimentOpt.isPresent())
		 * throw new Exception("PropertyType not found");
		 */
		
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
		
		validatePayload(payload);
		formatMobileNo(payload);
		Address address = lead.getAddress();
        address.setAddr_line1(payload.getAddressLine1());
		address.setAddr_line2(payload.getAddressLine2());
		address.setCity(payload.getCity());
		//address.setDistrict(payload.getDist());
		address.setPincode(payload.getPincode());
		address=aRepo.save(address);
		aRepo.flush();
		Long aid=address.getAddrId();
//			System.out.println(id);
		Optional<Address> addressOpt = aRepo.findById(aid);
		if(! addressOpt.isPresent())
			throw new Exception("Address not found");
			
		
		lead.setCustomerName(payload.getCustomerName());
		lead.setPrimaryMobile(payload.getPrimaryMobile());
		lead.setSecondaryMobile(payload.getSecondaryMobile());
		lead.setDateOfBirth(payload.getDateOfBirth());
		lead.setEmailId(payload.getEmailId());
		lead.setOccupation(payload.getOccupation());
		lead.setPurpose(payload.getPurpose());
		//lead.setPropertyType(propertyTypeOpt.get());
		lead.setSentiment(sentimentOpt.get());
		//lead.setSource(sourceOpt.get());
		lead.setBroker(brokerOpt.get());
		lead.setAddress(addressOpt.get());
		//lead.setUserId(userId);
		lRepo.save(lead);
		
		
		//check if leadid and statusid pair exists
		//if exists is it latest, if yes dont insert
		//if exists is not latest, insert
		//else insert
		//int exist=lsRepo.checkexist(id,payload.getStatusId());
//		System.out.println(exist);
		/*
		if(exist>0) {
			List<LeadStatus> latest=lsRepo.checklatest(id);
			Long statusid=latest.get(0).getStatusId();
			System.out.println(latest.get(0).getStatusId());
			if(statusid==payload.getStatusId()) {
			}else {
				LeadStatus lstatus=new LeadStatus();
				lstatus.setLeadId(id);
				lstatus.setStatusId(payload.getStatusId());
				lstatus.setUserId(userId);
				lsRepo.save(lstatus);
			}
		}else {
			LeadStatus lstatus=new LeadStatus();
			lstatus.setLeadId(id);
			lstatus.setStatusId(payload.getStatusId());
			lstatus.setUserId(userId);
			lsRepo.save(lstatus);
		}*/
		return lead;	
				
		
	}
		
	
	
	
}
