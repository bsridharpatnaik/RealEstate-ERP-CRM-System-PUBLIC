package com.ec.application.ReusableClasses;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.ec.application.data.ConsolidatedProductRow;
import com.ec.application.data.EmailConfigData;
import com.ec.application.data.ExpiryAlertRow;
import com.ec.application.data.JobFailureAlertDTO;
import com.ec.application.data.ProductStockRow;
import com.ec.application.data.ProjectStockEmailData;
import com.ec.application.data.StockDiscrepancyRow;
import com.ec.application.data.StockInformationExportDAO;
import com.ec.application.model.StockValidation;
import com.ec.application.service.EmailRecipientService;
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

	@Autowired
	EmailRecipientService emailRecipientService;

	Logger log = LoggerFactory.getLogger(EmailHelper.class);

	@Value("${stock.notification.emailids}")
	private String emailIds;

	@Value("${stock.validation.emailids}")
	private String validationEmailIds;
	
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
			model.put("tenantName",com.ec.application.multitenant.ThreadLocalStorage.getTenantName());
			Template template = config.getTemplate("email-template.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
			helper.setFrom(emailConfigData.mailUsername);
			
			InternetAddress[] parse = InternetAddress.parse(emailIds , true);
			message.setRecipients(javax.mail.Message.RecipientType.TO,  parse);
			
			helper.setSubject(ThreadLocalStorage.getTenantName()+" - Latest Stock Information - "+new Date());
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
			
			helper.setSubject(ThreadLocalStorage.getTenantName()+" - Stock Validation - "+new Date());
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
	
	public void sendStockBalanceValidationEmail(List<StockDiscrepancyRow> discrepancies) throws Exception
	{
		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = getProperties();
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
			}
		});
		MimeMessage message = new MimeMessage(session);
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			Map<String, Object> model = new HashMap<>();
			model.put("discrepancies", discrepancies);
			model.put("currentDate", new Date().toString());
			Template template = config.getTemplate("email-stock-balance-check.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
			helper.setFrom(emailConfigData.mailUsername);
			InternetAddress[] parse = InternetAddress.parse(validationEmailIds, true);
			message.setRecipients(javax.mail.Message.RecipientType.TO, parse);
			String subject = discrepancies.isEmpty()
					? "✅ Stock Balance OK - " + new Date()
					: "⚠ Stock Balance Discrepancies Found (" + discrepancies.size() + ") - " + new Date();
			helper.setSubject(subject);
			helper.setText(html, true);
			Transport.send(message);
			log.info("Stock balance validation email sent to {}", validationEmailIds);
		} catch (MessagingException e) {
			log.error("Error sending stock balance validation email", e);
			e.printStackTrace();
		}
	}

	public void sendJobFailureAlert(List<JobFailureAlertDTO> failures, String jobName) throws Exception
	{
		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = getProperties();
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
			}
		});
		MimeMessage message = new MimeMessage(session);
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					StandardCharsets.UTF_8.name());
			Map<String, Object> model = new HashMap<>();
			model.put("failures", failures);
			model.put("jobName", jobName);
			model.put("currentDate", new Date().toString());
			Template template = config.getTemplate("email-job-failure.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
			helper.setFrom(emailConfigData.mailUsername);
			InternetAddress[] parse = InternetAddress.parse(validationEmailIds, true);
			message.setRecipients(javax.mail.Message.RecipientType.TO, parse);
			helper.setSubject("⚠ Job Failure: " + jobName + " (" + failures.size() + " failure(s)) - " + new Date());
			helper.setText(html, true);
			Transport.send(message);
			log.info("Job failure alert sent for job: {}", jobName);
		} catch (MessagingException e) {
			log.error("Error sending job failure alert email", e);
			e.printStackTrace();
		}
	}

	public void sendDailyStockReport(List<ProjectStockEmailData> projects, byte[] excelBytes,
			List<ExpiryAlertRow> expiring30, List<ExpiryAlertRow> expiring60) throws Exception {
		String recipients = emailRecipientService.getRecipientsAsString(EmailRecipientService.DAILY_STOCK_REPORT);
		if (recipients == null || recipients.isEmpty()) {
			log.warn("No active recipients configured for daily_stock_report — skipping email");
			return;
		}

		EmailConfigData emailConfigData = emailService.getEmailConfig();
		Properties props = getProperties();
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailConfigData.mailUsername, emailConfigData.mailPassword);
			}
		});

		MimeMessage message = new MimeMessage(session);
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

			Map<String, Object> model = new HashMap<>();
			model.put("projects", projects);
			model.put("currentDate",
					new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date()));

			// Build cross-project consolidated product table
			List<String> projectNames = new ArrayList<>();
			for (ProjectStockEmailData p : projects) projectNames.add(p.getProjectName());

			TreeSet<String> allProductNames = new TreeSet<>();
			for (ProjectStockEmailData p : projects) {
				for (ProductStockRow r : p.getStockRows()) allProductNames.add(r.getProductName());
				allProductNames.addAll(p.getZeroStockItems());
			}

			List<ConsolidatedProductRow> consolidatedRows = new ArrayList<>();
			for (String productName : allProductNames) {
				List<Double> qtys = new ArrayList<>();
				double rowTotal = 0;
				for (ProjectStockEmailData p : projects) {
					double qty = p.getStockRows().stream()
							.filter(r -> r.getProductName().equals(productName))
							.mapToDouble(ProductStockRow::getTotalQty)
							.findFirst().orElse(0.0);
					qty = BigDecimal.valueOf(qty).setScale(2, RoundingMode.HALF_UP).doubleValue();
					qtys.add(qty);
					rowTotal += qty;
				}
				rowTotal = BigDecimal.valueOf(rowTotal).setScale(2, RoundingMode.HALF_UP).doubleValue();
				consolidatedRows.add(new ConsolidatedProductRow(productName, qtys, rowTotal));
			}

			model.put("projectNames", projectNames);
			model.put("consolidatedRows", consolidatedRows);
			model.put("expiring30", expiring30 != null ? expiring30 : new java.util.ArrayList<>());
			model.put("expiring60", expiring60 != null ? expiring60 : new java.util.ArrayList<>());

			Template template = config.getTemplate("email-daily-stock.ftl");
			String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

			helper.setFrom(emailConfigData.mailUsername);
			message.setRecipients(javax.mail.Message.RecipientType.TO,
					InternetAddress.parse(recipients, true));
			helper.setSubject("Daily Stock Report — " +
					new java.text.SimpleDateFormat("dd MMM yyyy").format(new Date()));
			helper.setText(html, true);

			String fileName = "Stock_Report_" +
					new java.text.SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";
			javax.mail.util.ByteArrayDataSource ds = new javax.mail.util.ByteArrayDataSource(
					excelBytes,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			helper.addAttachment(fileName, ds);

			Transport.send(message);
			log.info("Daily stock report sent to {}", recipients);
		} catch (MessagingException e) {
			log.error("Error sending daily stock report email", e);
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
