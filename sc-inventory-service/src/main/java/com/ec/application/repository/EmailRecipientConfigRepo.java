package com.ec.application.repository;

import com.ec.application.model.EmailRecipientConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailRecipientConfigRepo extends JpaRepository<EmailRecipientConfig, Long> {
    List<EmailRecipientConfig> findByEmailTypeAndActiveTrue(String emailType);
    boolean existsByEmailTypeAndEmailAddress(String emailType, String emailAddress);
}
