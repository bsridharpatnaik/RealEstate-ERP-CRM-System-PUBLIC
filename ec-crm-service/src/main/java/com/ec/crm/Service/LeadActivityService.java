package com.ec.crm.Service;

import java.util.List;
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

import com.ec.crm.Data.LeadActivityCreate;
import com.ec.crm.Data.PropertyTypeListWithTypeAheadData;
import com.ec.crm.Data.SourceListWithTypeAheadData;
import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.PropertyTypeSpecifications;
import com.ec.crm.Filters.SourceSpecifications;
import com.ec.crm.Model.ActivityType;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.Model.PropertyType;
import com.ec.crm.Repository.ActivityTypeRepo;
import com.ec.crm.Repository.LeadActivityRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.ReusableClasses.CommonUtils;
import com.ec.crm.ReusableClasses.IdNameProjections;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LeadActivityService {
	@Autowired
	LeadActivityRepo laRepo;
	
	@Autowired
	LeadRepo lRepo;
	
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Value("${common.serverurl}")
	private String reqUrl;
	
	CommonUtils utilObj = new CommonUtils();
	
	@Autowired
	ActivityTypeRepo aRepo;
	
	public Page<LeadActivity> fetchAll(Pageable pageable) 
	{
		return laRepo.findAll(pageable);
	}

	public LeadActivity createLeadActivity(@Valid LeadActivityCreate la) throws Exception {
		Optional<Lead> leadOpt = lRepo.findById(la.getLeadId());
		if(! leadOpt.isPresent())
			throw new Exception("lead not found");
		
		Optional<ActivityType> atOpt = aRepo.findById(la.getActivityTypeId());
		if(! atOpt.isPresent())
			throw new Exception("ActivityType not found");
		
		Long userId;
		UserReturnData userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/me")
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData.class)
		    	.block();
		userId= userDetails.getId();
		
		LeadActivity lat=new LeadActivity();
		lat.setActivityDate(la.getActivityDate());
		lat.setCreationDate(la.getCreationDate());
		lat.setDescription(la.getDescription());
		lat.setTags(la.getTags());
		lat.setStatus(lat.getStatus());
		lat.setTitle(la.getTitle());
		lat.setTime(la.getTime());
		lat.setUserId(userId);
		lat.setLead(leadOpt.get());
		lat.setActivityType(atOpt.get());
		return laRepo.save(lat);
		

		
	}
	
	public LeadActivity findSingleLeadActivity(long id) throws Exception 
	{
		Optional<LeadActivity> latype = laRepo.findById(id);
		if(latype.isPresent())
			return latype.get();
		else
			throw new Exception("LeadActivity ID not found");
	}
	
	public LeadActivity updateLeadActivity(Long id, LeadActivityCreate la) throws Exception 
	{
		Optional<LeadActivity> LeadActivityForUpdateOpt = laRepo.findById(id);
		LeadActivity LeadActivityForUpdate = LeadActivityForUpdateOpt.get();
		
		if(!LeadActivityForUpdateOpt.isPresent())
			throw new Exception("LeadActivity not found with activityTypeid");
		
		Optional<Lead> leadOpt = lRepo.findById(la.getLeadId());
		if(! leadOpt.isPresent())
			throw new Exception("lead not found");
		
		Optional<ActivityType> atOpt = aRepo.findById(la.getActivityTypeId());
		if(! atOpt.isPresent())
			throw new Exception("ActivityType not found");
		
		Long userId;
		UserReturnData userDetails = webClientBuilder.build()
		    	.get()
		    	.uri(reqUrl+"user/me")
		    	.header("Authorization", request.getHeader("Authorization"))
		    	.retrieve()
		    	.bodyToMono(UserReturnData.class)
		    	.block();
		userId= userDetails.getId();
		
		LeadActivityForUpdate.setActivityDate(la.getActivityDate());
		LeadActivityForUpdate.setCreationDate(la.getCreationDate());
		LeadActivityForUpdate.setDescription(la.getDescription());
		LeadActivityForUpdate.setTags(la.getTags());
		LeadActivityForUpdate.setStatus(la.getStatus());
		LeadActivityForUpdate.setTitle(la.getTitle());
		LeadActivityForUpdate.setTime(la.getTime());
		LeadActivityForUpdate.setUserId(userId);
		LeadActivityForUpdate.setLead(leadOpt.get());
		LeadActivityForUpdate.setActivityType(atOpt.get());
		return laRepo.save(LeadActivityForUpdate);
	}
	
	public void deleteLeadActivity(Long id) throws Exception 
	{
		laRepo.softDeleteById(id);
	}
	
}
