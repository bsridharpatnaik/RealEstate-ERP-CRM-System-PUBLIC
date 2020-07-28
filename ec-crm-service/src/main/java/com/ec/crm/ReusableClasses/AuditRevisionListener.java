package com.ec.crm.ReusableClasses;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		System.out.println("Before audit APi call" +dtf.format(now));  
        UserReturnData userReturnData = userDetailsService.getCurrentUser();
        now = LocalDateTime.now();  
		System.out.println("after audit APi call" +dtf.format(now));
        Long userId = userReturnData.getId();
        String userName = userReturnData.getUsername();
        audit.setUserId(userId);
        audit.setUserName(userName);
    }
}
