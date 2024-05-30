package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.*;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.crm.Enums.LoanStatusEnum;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Enums.PropertyTypeEnum;
import com.ec.crm.Enums.SentimentEnum;
import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

@Entity
@Table(name = "customer_lead")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited(withModifiedFlag = true)
@Data
public class Lead extends ReusableFields implements Serializable {
    public Lead() {
    }

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "lead_id", updatable = false, nullable = false)
    Long leadId;

    @Column(name = "name")
    String customerName;

    @Column(name = "primary_mobile")
    String primaryMobile;

    @Column(name = "secondary_mobile")
    String secondaryMobile;

    @Column(name = "email_id")
    String emailId;

    @Column(name = "purpose")
    String purpose;

    @Column(name = "occupation")
    String occupation;

    @Column(name = "dateofbirth")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date dateOfBirth;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "broker_id", nullable = true)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    Broker broker;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = true)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    Address address;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "source_id", nullable = true)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    Source source;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    PropertyTypeEnum propertyType;

    @Column
    @Enumerated(EnumType.STRING)
    SentimentEnum sentiment;

    @NotAudited
    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

    @NotAudited
    @Column(name = "stagnantDaysCount")
    Long stagnantDaysCount;

    @Column(name = "user_id")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long asigneeId;

    @Column(name = "created_by")
    @JsonSerialize(using = ToUsernameSerializer.class)
    Long creatorId;

    @NonNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    LeadStatusEnum status;

    @Column(name = "is_prospect_lead")
    @ColumnDefault("false")
    Boolean isProspectLead;

    @NotAudited
    @Column(name="loanStatus")
    String loanStatus;

    @NotAudited
    @Column(name="customerStatus")
    String customerStatus;

    @NotAudited
    @Column(name="nextPaymentDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date nextPaymentDate;

    @NotAudited
    @Column(name="totalPending")
    Double totalPending;

    @NotAudited
    @Column(name="recentIsOpen")
    Boolean recentIsOpen;

    @NotAudited
    @Column(name="recentActivityDateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date recentActivityDateTime;
}
