package com.ec.application.ReusableClasses;

import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.data.EmailConfigData;
import com.ec.application.service.EmailService;

@Service
public class EmailHelper 
{
	@Autowired 
	EmailService emailService;
	
	Logger log = LoggerFactory.getLogger(EmailHelper.class);
	
	public void sendEmail()
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

        log.info("Fetching Session");
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
            }
        });
        log.info("Creating mimemessage");
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(emailConfigData.mailUsername, false));
            msg.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse("bsridharpatnaik@gmail.com"));
            msg.setSubject("Test Subject.");
            msg.setContent("Test Content.", "text/html");
            msg.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent("Test Content.", "text/html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            //MimeBodyPart attachPart = new MimeBodyPart();

           // attachPart.attachFile("/var/tmp/abc.txt");
           // multipart.addBodyPart(attachPart);
            msg.setContent(multipart);
            log.info("Sending Email");
            Transport.send(msg);
            log.info("Email Sent");
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
        	log.error("Error sending email", e);
            e.printStackTrace();
        }
	}
}
