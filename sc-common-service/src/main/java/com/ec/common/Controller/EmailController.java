package com.ec.common.Controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Configuration.EmailConstants;
import com.ec.common.Data.EmailConfigData;
import com.ec.common.Data.UserListWithTypeAheadData;

@RestController
@RequestMapping(value="/emailconfig",produces = { "application/json", "text/json" })
public class EmailController 
{
	@GetMapping
	public EmailConfigData returnEmailConfig() 
	{
		
		return new EmailConfigData(EmailConstants.mailHost,EmailConstants.mailPort,EmailConstants.mailUsername,EmailConstants.mailPassword
				,EmailConstants.mailProtocol,EmailConstants.mailSmtpAuth,EmailConstants.mailSmtpSslEnable,EmailConstants.mailSmtpSslTrust);
	}
}
