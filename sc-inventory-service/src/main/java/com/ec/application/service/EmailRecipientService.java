package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.EmailRecipientConfig;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.EmailRecipientConfigRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailRecipientService {

    public static final String DAILY_STOCK_REPORT = "daily_stock_report";

    @Autowired
    private EmailRecipientConfigRepo repo;

    @Autowired
    private SchemaConfig schemaConfig;

    Logger log = LoggerFactory.getLogger(EmailRecipientService.class);

    @PostConstruct
    @Transactional
    public void seedDefaultEmails() {
        ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
        try {
            seedIfAbsent(DAILY_STOCK_REPORT, "info@mahavirgroupindia.com");
            seedIfAbsent(DAILY_STOCK_REPORT, "purchase@mahavirgroupindia.com");
            seedIfAbsent(DAILY_STOCK_REPORT, "sridhar@mahavirgroupindia.com");
            seedIfAbsent(DAILY_STOCK_REPORT, "purchasemanager@mahavirgroupindia.com");
            log.info("Email recipient defaults seeded for type: {}", DAILY_STOCK_REPORT);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    /**
     * Returns comma-separated active email addresses for the given type.
     * Always reads from master schema. Saves and restores the previous
     * tenant context so this is safe to call from any tenant context.
     */
    public String getRecipientsAsString(String emailType) {
        String previousTenant = ThreadLocalStorage.getTenantName();
        ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
        try {
            List<EmailRecipientConfig> configs = repo.findByEmailTypeAndActiveTrue(emailType);
            return configs.stream()
                    .map(EmailRecipientConfig::getEmailAddress)
                    .collect(Collectors.joining(","));
        } finally {
            ThreadLocalStorage.setTenantName(previousTenant);
        }
    }

    private void seedIfAbsent(String emailType, String emailAddress) {
        if (!repo.existsByEmailTypeAndEmailAddress(emailType, emailAddress)) {
            repo.save(new EmailRecipientConfig(emailType, emailAddress));
            log.info("Seeded email recipient: {} -> {}", emailType, emailAddress);
        }
    }
}
