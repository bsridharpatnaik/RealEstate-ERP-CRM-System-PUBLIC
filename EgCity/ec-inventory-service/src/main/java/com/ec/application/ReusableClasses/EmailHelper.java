package com.ec.application.ReusableClasses;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.ec.application.data.EmailConfigData;
import com.ec.application.data.StockInformationExportDAO;
import com.ec.application.model.StockValidation;
import com.ec.application.service.EmailService;
import com.ec.application.service.StockService;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Service
public class EmailHelper 
{
	@Autowired 
	EmailService emailService;
	
	@Autowired
	private Configuration config;
	
	@Autowired
	StockService stockService;
	Logger log = LoggerFactory.getLogger(EmailHelper.class);
	
	@Value("${stock.notification.emailids}") 
	private String emailIds;
	
	public void sendEmailForMorningStockNottification(List<StockInformationExportDAO> dataForInsertList) throws Exception
	{
		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = getProperties();
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
            }
        });
        log.info("Creating mimemessage");
        MimeMessage message = new MimeMessage(session);
        try {
        	MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
        	Map<String, Object> model = new HashMap<String, Object>();
			model.put("inventory", dataForInsertList);
			model.put("currentDate", new Date().toString());
			Template template = config.getTemplate("email-template.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
			helper.setFrom(emailConfigData.mailUsername);
			
			InternetAddress[] parse = InternetAddress.parse(emailIds , true);
			message.setRecipients(javax.mail.Message.RecipientType.TO,  parse);
			
			helper.setSubject("Latest Stock Information - "+new Date());
			helper.setText(html, true);
			Transport.send(message);
            log.info("Email Sent");
        } 
        catch (MessagingException e) 
        {
            // TODO Auto-generated catch block
        	log.error("Error sending email", e);
            e.printStackTrace();
        }
	}
	
	public void sendEmailForStockValidation(List<StockValidation> dataForEmail) throws Exception
	{
		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = getProperties();
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
            }
        });
        log.info("Creating mimemessage");
        MimeMessage message = new MimeMessage(session);
        try {
        	MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
        	Map<String, Object> model = new HashMap<String, Object>();
			model.put("inventory", dataForEmail);
			model.put("currentDate", new Date().toString());
			Template template = config.getTemplate("email-stockValidation.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
			helper.setFrom(emailConfigData.mailUsername);
			
			InternetAddress[] parse = InternetAddress.parse(emailIds , true);
			log.info("Email trigerred for - "+javax.mail.Message.RecipientType.TO);
			message.setRecipients(javax.mail.Message.RecipientType.TO,  parse);
			
			helper.setSubject("Stock Validation - "+new Date());
			helper.setText(html, true);
			Transport.send(message);
            log.info("Email Sent");
        } 
        catch (MessagingException e) 
        {
            // TODO Auto-generated catch block
        	log.error("Error sending email", e);
            e.printStackTrace();
        }
	}
	
	private Properties getProperties()
	{
		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = new Properties();
        props.put("mail.host", emailConfigData.mailHost);
        props.put("mail.port", emailConfigData.mailPort);
        props.put("mail.username", emailConfigData.mailUsername);
        props.put("mail.password", emailConfigData.mailPassword);
        props.put("mail.protocol", emailConfigData.mailProtocol);

        props.put("mail.smtp.auth", emailConfigData.mailSmtpAuth);
        props.put("mail.smtp.ssl.enable", emailConfigData.mailSmtpSslEnable);
        props.put("mail.smtp.ssl.trust", emailConfigData.mailSmtpSslTrust);
        return props;
	}
}
