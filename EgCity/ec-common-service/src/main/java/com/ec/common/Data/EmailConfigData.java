package com.ec.common.Data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class EmailConfigData 
{
	public String mailHost;
	public String mailPort;
	public String mailUsername;
	public String mailPassword;
	public String mailProtocol;
	public String mailSmtpAuth;
	public String mailSmtpSslEnable;
	public String mailSmtpSslTrust;
	
	public EmailConfigData(String mailHost, String mailPort, String mailUsername, String mailPassword,
			String mailProtocol, String mailSmtpAuth, String mailSmtpSslEnable, String mailSmtpSslTrust) {
		super();
		this.mailHost = mailHost;
		this.mailPort = mailPort;
		this.mailUsername = mailUsername;
		this.mailPassword = mailPassword;
		this.mailProtocol = mailProtocol;
		this.mailSmtpAuth = mailSmtpAuth;
		this.mailSmtpSslEnable = mailSmtpSslEnable;
		this.mailSmtpSslTrust = mailSmtpSslTrust;
	}
	
	
}
