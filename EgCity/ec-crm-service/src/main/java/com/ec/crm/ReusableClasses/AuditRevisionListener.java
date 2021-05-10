package com.ec.crm.ReusableClasses;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Service.UserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditRevisionListener implements RevisionListener
{
	@Autowired
	ApplicationContext ctx;

	@Autowired
	HttpServletRequest request;

	Logger log = LoggerFactory.getLogger(AuditRevisionListener.class);

	@Override
	public void newRevision(Object revisionEntity)
	{
		UserDetailsService userDetailsService = ctx.getBean(UserDetailsService.class);

		AuditRevisionEntity audit = (AuditRevisionEntity) revisionEntity;
		log.info("Fetching user details before submitting revision information");
		UserReturnData userReturnData;
		try
		{
			userReturnData = userDetailsService.getCurrentUser();
			log.info("Username fetched from common-service - " + userReturnData.getUsername());
			Long userId = userReturnData.getId();
			String userName = userReturnData.getUsername();
			audit.setUserId(userId);
			audit.setUserName(userName);
			log.info("Data set for autit revision");
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
