package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE + " AND status='Deal_Closed'")
@Audited(withModifiedFlag = true)
@Data
public class ClosedLeads extends ReusableFields implements Serializable
{

	public ClosedLeads()
	{
		super();
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
	{ "hibernateLazyInitializer", "handler" })
	Broker broker;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "address_id", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Address address;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "source_id", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Source source;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	PropertyTypeEnum propertyType;

	@Column
	@Enumerated(EnumType.STRING)
	SentimentEnum sentiment;

	@NotAudited
	@Formula("(Select max(la.updated_at) from LeadActivity la Where la.lead_id=lead_id and la.is_deleted=false)")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date lastActivityModifiedDate;

	@NotAudited
	@Formula("(SELECT CASE WHEN l.status in ('Deal_closed','Deal_Lost') "
			+ "THEN 0 ELSE datediff(now(),max(la.updated_at)) END FROM LeadActivity la "
			+ "INNER JOIN customer_lead l on l.lead_id=la.lead_id WHERE la.lead_id=lead_id AND la.is_deleted=0)")
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

    @NotAudited
    @Formula("(select cds.loanStatus from customer_deal_structure cds " +
            "INNER JOIN customer_lead cl on cl.lead_id = cds.lead_id " +
            "WHERE " +
            "cds.is_deleted=false " +
            "AND cl.is_deleted=false " +
            "AND cl.lead_id=lead_id " +
            "AND cds.loanStatus IS NOT NULL " +
            "LIMIT 1)")
    String loanStatus;

	@NotAudited
	@Formula("(select cds.customerStatus from customer_deal_structure cds " +
			"INNER JOIN customer_lead cl on cl.lead_id = cds.lead_id " +
			"WHERE " +
			"cds.is_deleted=false " +
			"AND cl.is_deleted=false " +
			"AND cl.lead_id=lead_id " +
			"AND cds.customerStatus IS NOT NULL " +
			"LIMIT 1)")
	String customerStatus;
}
