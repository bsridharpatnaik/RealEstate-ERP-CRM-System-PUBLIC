package com.ec.application.Audit;


import com.ec.application.service.UserDetailsService;
import org.hibernate.envers.RevisionListener;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditRevisionListener implements RevisionListener 
{
	@Autowired
	UserDetailsService userDetailsService;
	
    @Override
    public void newRevision(Object revisionEntity) 
    {
        AuditRevisionEntity audit = (AuditRevisionEntity) revisionEntity;
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = userDetailsService.getCurrentUser().getUsername();
        audit.setUsername(username);
    }
}
