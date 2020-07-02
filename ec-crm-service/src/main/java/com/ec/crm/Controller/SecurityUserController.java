package com.ec.crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.ec.crm.Model.SecurityUser;
import com.ec.crm.Service.SecurityUserService;

@RestController
@RequestMapping(value="/securityuser",produces = { "application/json", "text/json" })
public class SecurityUserController {
	@Autowired
	SecurityUserService securityUserService;
	
	@GetMapping
	public Page<SecurityUser> returnAllSecurityUser(Pageable pageable) 
	{
		return securityUserService.fetchAll(pageable);
	}
	
	@GetMapping("/{id}")
	public SecurityUser findSecurityUserByID(@PathVariable long id) throws Exception 
	{
		return securityUserService.findSingleSecurityUser(id);
	}
	
	@PostMapping("/create") 
	@ResponseStatus(HttpStatus.CREATED)
	public SecurityUser createSentiment(@RequestBody SecurityUser securityuser) throws Exception{
		
		return securityUserService.createSecurityUser(securityuser);
	}
	
	@PutMapping("/{id}")
	public SecurityUser updateSecurityUser(@PathVariable Long id, @RequestBody SecurityUser securityuser) throws Exception 
	{
		return securityUserService.updateSecurityUser(id, securityuser);
	} 
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteSecurityUser(@PathVariable Long id) throws Exception
	{
		securityUserService.deleteSecurityUser(id);
			return ResponseEntity.ok("Source Deleted sucessfully.");
	}
}
