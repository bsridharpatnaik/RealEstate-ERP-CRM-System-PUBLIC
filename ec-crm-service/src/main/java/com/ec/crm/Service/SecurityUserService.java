package com.ec.crm.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
//import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.ec.crm.Model.SecurityUser;
import com.ec.crm.Repository.SecurityUserRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecurityUserService {
	@Autowired
	SecurityUserRepo sRepo;
	public Page<SecurityUser> fetchAll(Pageable pageable) 
	{
		return sRepo.findAll(pageable);
	}
	
	public SecurityUser findSingleSecurityUser(long id) throws Exception 
	{
		Optional<SecurityUser> securityuser = sRepo.findById(id);
		if(securityuser.isPresent())
			return securityuser.get();
		else
			throw new Exception("securityuser ID not found");
	}
	public SecurityUser createSecurityUser(SecurityUser securityuser) throws Exception {
		String password = securityuser.getPassword();
		securityuser.setPassword(bCryptPassword(password));
		sRepo.save(securityuser);
		return securityuser;
	}
	public String bCryptPassword(String password) {
		String bcyptedPassword;
		//bcyptedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
		return null;
	}
	public SecurityUser updateSecurityUser(Long id, SecurityUser securityuser) throws Exception 
	{
		String password = securityuser.getPassword();
		securityuser.setPassword(bCryptPassword(password));
		sRepo.save(securityuser);
	    return securityuser;
	}
	
	public void deleteSecurityUser(Long id) throws Exception 
	{
		sRepo.softDeleteById(id);
	}
	
}
