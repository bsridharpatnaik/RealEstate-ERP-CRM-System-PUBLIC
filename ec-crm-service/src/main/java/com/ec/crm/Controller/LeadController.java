package com.ec.crm.Controller;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Data.LeadDetailInfo;
import com.ec.crm.Data.LeadListWithTypeAheadData;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.LeadStatusEnum;
import com.ec.crm.Model.PropertyTypeEnum;
import com.ec.crm.Service.LeadService;

@RestController
@RequestMapping(value="/lead",produces = { "application/json", "text/json" })
public class LeadController {
	@Autowired
	LeadService leadService;
	
	@GetMapping
	public Page<Lead> returnAllSentiment(Pageable pageable) 
	{
		return leadService.fetchAll(pageable);
	}
	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public LeadListWithTypeAheadData returnFilteredLeads(@RequestBody FilterDataList leadFilterDataList,@PageableDefault(page = 0, size = 10, sort = "leadId", direction = Direction.DESC) Pageable pageable) 
	{
		return leadService.findFilteredList(leadFilterDataList,pageable);
	}
	@GetMapping("/{id}")
	public Lead findLeadByID(@PathVariable long id) throws Exception 
	{
		return leadService.findSingleLead(id);
	}
	
	@GetMapping("/getallinfo/{id}")
	public LeadDetailInfo findLeadDetailInfoByID(@PathVariable long id) throws Exception 
	{
		return leadService.findSingleLeadDetailInfo(id);
	}
	
	@GetMapping("/validPropertyTypes")
	public List<String> findValidPropertyTypes() 
	{
		return PropertyTypeEnum.getValidPropertyType();
	}
	
	@GetMapping("/validLeadStatus")
	public List<String> findValidLeadStatus() 
	{
		return LeadStatusEnum.getValidLeadStatus();
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public Lead createLead(@Valid @RequestBody LeadCreateData payload) throws Exception{
		
		return leadService.createLead(payload);
	}
	
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<?> deleteProduct(@PathVariable Long id) throws Exception
	{
		leadService.deleteLead(id);
		return ResponseEntity.ok("Entity deleted");
	}
	
	@PutMapping("/{id}")
	public Lead updateLead(@PathVariable Long id, @RequestBody LeadCreateData payload) throws Exception 
	{
		return leadService.updateLead(id, payload);
	} 
}
