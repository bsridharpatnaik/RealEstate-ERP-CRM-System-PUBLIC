package com.ec.application.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.ec.application.data.UserReturnData;
import com.ec.application.service.UserDetailsService;

@Component
public class AuditorAwareImpl implements AuditorAware<String>
{

	@Autowired
	UserDetailsService userDetailsService;

	@Override
	public Optional<String> getCurrentAuditor()
	{
		UserReturnData currentUser;
		try
		{
			currentUser = userDetailsService.getCurrentUser();
			return Optional.of(currentUser.getUsername());
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Optional.of("");
		}
	}
}