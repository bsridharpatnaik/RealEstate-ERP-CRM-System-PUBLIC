package com.ec.application.service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.application.config.EmailConstants;
import com.ec.application.data.EmailConfigData;

@Service
@Transactional
public class EmailService
{

	@Autowired
	WebClient.Builder webClientBuilder;

	@Autowired
	HttpServletRequest request;

	@Value("${common.serverurl}")
	private String reqUrl;

	Logger log = LoggerFactory.getLogger(EmailService.class);

	public EmailConfigData getEmailConfig()
	{
		log.info("Getting email configuration from master");
		EmailConfigData emailConfigData = new EmailConfigData(EmailConstants.mailHost, EmailConstants.mailPort,
				EmailConstants.mailUsername, EmailConstants.mailPassword, EmailConstants.mailProtocol,
				EmailConstants.mailSmtpAuth, EmailConstants.mailSmtpSslEnable, EmailConstants.mailSmtpSslTrust);

		/*
		 * webClientBuilder.build() .get() .uri(reqUrl+"/emailconfig")
		 * .header("Authorization", request.getHeader("Authorization")) .retrieve()
		 * .bodyToMono(EmailConfigData.class) .block();
		 * log.info("Fetched email configuration from master "+emailConfigData.toString(
		 * ));
		 */
		return emailConfigData;
	}
}
