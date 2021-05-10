package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Entity

@Table(name = "customer_payment_schedule")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited(withModifiedFlag = true)
@Data
public class PaymentSchedule extends ReusableFields implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "schedule_id", updatable = false, nullable = false)
	Long scheduleId;

	@Column(name = "payment_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentDate;

	@Column(name = "mode")
	String mode;

	@Column(name = "amount")
	Double amount;

	@Column(name = "details", length = 150)
	@Size(max = 150)
	String details;

	@Column(name = "isReceived")
	Boolean isReceived;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "deal_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	DealStructure ds;

	@NotAudited
	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "leadactivity_id", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	LeadActivity la;

	@Column(nullable = false)
	Boolean isCustomerPayment;
}
