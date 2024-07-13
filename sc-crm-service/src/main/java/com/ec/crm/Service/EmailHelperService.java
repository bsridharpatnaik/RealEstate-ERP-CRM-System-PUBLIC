package com.ec.crm.Service;

import com.ec.crm.Config.EmailConstants;
import com.ec.crm.Data.ActivityForEmail;
import com.ec.crm.Data.EmailConfigData;
import com.ec.crm.multitenant.ThreadLocalStorage;
import freemarker.core.ParseException;
import freemarker.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EmailHelperService {
    @Autowired
    private Configuration config;

    Logger log = LoggerFactory.getLogger(EmailHelperService.class);

    public void sendEmail(Map<String, Object> model, String recipientList, String subject, String key) {
        EmailConfigData emailConfigData = getEmailConfig();
        Properties props = getProperties();
        Session session = createEmailSession(emailConfigData, props);
        log.info("Creating mime message");
        MimeMessage message = new MimeMessage(session);
        try {
            prepareAndSendEmail(message, model, emailConfigData, recipientList, subject, key);
            log.info("Email Sent");
        } catch (Exception e) {
            log.error("Error sending email", e);
            e.printStackTrace();
        }
    }

    private Session createEmailSession(EmailConfigData emailConfigData, Properties props) {
        return Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
            }
        });
    }

    private void prepareAndSendEmail(MimeMessage message, Map<String, Object> model, EmailConfigData emailConfigData, String recipientList, String subject, String key) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        String html = generateEmailContent(model, key);
        helper.setFrom(emailConfigData.mailUsername);
        InternetAddress[] recipients = parseRecipientList(recipientList);
        message.setRecipients(javax.mail.Message.RecipientType.TO, recipients);
        helper.setSubject(ThreadLocalStorage.getTenantName() + " - " + subject + " - " + new Date());
        helper.setText(html, true);
        Transport.send(message);
    }

    private String generateEmailContent(Map<String, Object> model, String key) throws Exception {
        Template template = getTemplateByKey(key);
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }

    private Template getTemplateByKey(String key) throws IOException {
        Template template = null;
        switch (key) {
            case "upcomingEmailForLeadActivity":
                template = config.getTemplate("lead-activity-list.ftl");
                break;
        }
        return template;
    }


    private InternetAddress[] parseRecipientList(String recipientList) throws Exception {
        String[] recipientArray = recipientList.split(";");
        InternetAddress[] recipients = new InternetAddress[recipientArray.length];
        for (int i = 0; i < recipientArray.length; i++) {
            recipients[i] = new InternetAddress(recipientArray[i].trim());
        }
        return recipients;
    }

    private Properties getProperties() {
        EmailConfigData emailConfigData = getEmailConfig();
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

    public EmailConfigData getEmailConfig() {
        log.info("Getting email configuration from master");
        return new EmailConfigData(EmailConstants.mailHost, EmailConstants.mailPort,
                EmailConstants.mailUsername, EmailConstants.mailPassword, EmailConstants.mailProtocol,
                EmailConstants.mailSmtpAuth, EmailConstants.mailSmtpSslEnable, EmailConstants.mailSmtpSslTrust);
    }
}
