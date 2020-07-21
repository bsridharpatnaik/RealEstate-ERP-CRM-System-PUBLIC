package com.ec.crm.ReusableClasses;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.envers.RevisionListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Service.UserDetailsService;

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
        UserReturnData userReturnData = userDetailsService.getCurrentUser();
        Long userId = userReturnData.getId();
        String userName = userReturnData.getUsername();
        audit.setUserId(userId);
        audit.setUserName(userName);
    }
}
