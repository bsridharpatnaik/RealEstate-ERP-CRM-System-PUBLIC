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
	UserDetailsService userDetailsService;

	@Autowired
	ApplicationContext ctx;

	@Override
	public void newRevision(Object revisionEntity)
	{
		UserDetailsService userDetailsService = ctx.getBean(UserDetailsService.class);

		AuditRevisionEntity audit = (AuditRevisionEntity) revisionEntity;
		UserReturnData userReturnData;
		try
		{
			userReturnData = userDetailsService.getCurrentUser();
			Long userId = userReturnData.getId();
			String userName = userReturnData.getUsername();
			audit.setUserId(userId);
			audit.setUserName(userName);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}