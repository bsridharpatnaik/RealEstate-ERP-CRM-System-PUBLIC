package com.ec.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * Stores email recipients by type. Lives in master schema.
 * Add/remove rows directly in DB — no redeploy needed.
 */
@Entity
@Table(name = "email_recipient_config")
@Getter @Setter @NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmailRecipientConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_type", nullable = false)
    private String emailType;   // e.g. "daily_stock_report"

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    public EmailRecipientConfig(String emailType, String emailAddress) {
        this.emailType = emailType;
        this.emailAddress = emailAddress;
        this.active = true;
    }
}
