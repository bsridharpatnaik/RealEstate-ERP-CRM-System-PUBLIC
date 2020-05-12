package com.ec.application.ReusableClasses;


import com.ec.application.service.UserDetailsService;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.envers.RevisionListener;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditRevisionListener implements RevisionListener 
{
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	HttpServletRequest request;
	
    @Override
    public void newRevision(Object revisionEntity) 
    {
        AuditRevisionEntity audit = (AuditRevisionEntity) revisionEntity;
        
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = userDetailsService.getCurrentUser().getUsername();
        
        audit.setUsername(username);
    }
}
