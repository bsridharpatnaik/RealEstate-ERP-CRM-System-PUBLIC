package com.ec.crm.Service;

import com.ec.crm.Config.EmailConstants;
import com.ec.crm.Data.ActivityForEmail;
import com.ec.crm.Data.EmailConfigData;
import com.ec.crm.multitenant.ThreadLocalStorage;
import freemarker.template.Configuration;
import freemarker.template.Template;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EmailHelperService {
    @Autowired
    private Configuration config;

    Logger log = LoggerFactory.getLogger(EmailHelperService.class);

    public void sendEmailForMorningStockNotsification() throws Exception {

        EmailConfigData emailConfigData = getEmailConfig();
        Properties props = getProperties();
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
            }
        });

        log.info("Creating mimemessage");
        MimeMessage message = new MimeMessage(session);
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            // Hardcoding sample values for the table
            Map<String, Object> model = new HashMap<>();
            model.put("leads", Arrays.asList(
                    new ActivityForEmail("L001", "John Doe", "1234567890", "Web", "Apartment", "Agent A", "New", "2024-07-12 10:00 AM", "Initial Contact", "Discussed property details", "Yes", "Call", "Yes", 1),
                    new ActivityForEmail("L002", "Jane Smith", "0987654321", "Referral", "House", "Agent B", "In Progress", "2024-07-11 02:30 PM", "Site Visit", "Visited the property", "No", "Meeting", "No", 2)
            ));

            Template template = config.getTemplate("email-template.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            helper.setFrom(emailConfigData.mailUsername);

            InternetAddress[] parse = InternetAddress.parse("bsridharpatnaik@gmail.com", true);
            message.setRecipients(javax.mail.Message.RecipientType.TO, parse);

            helper.setSubject(ThreadLocalStorage.getTenantName() + " - Latest Stock Information - " + new Date());
            helper.setText(html, true);
            Transport.send(message);
            log.info("Email Sent");
        } catch (MessagingException e) {
            log.error("Error sending email", e);
            e.printStackTrace();
        }
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
