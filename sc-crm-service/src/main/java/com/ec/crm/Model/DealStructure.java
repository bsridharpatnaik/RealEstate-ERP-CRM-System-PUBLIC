
package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.Size;

import com.ec.crm.Enums.CustomerStatusEnum;
import com.ec.crm.Enums.LoanStatusEnum;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import Deserializers.ToTitleCaseDeserializer;
import lombok.Data;

@Entity

@Table(name = "customer_deal_structure")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited(withModifiedFlag = true)
@Data
public class
DealStructure extends ReusableFields implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "deal_id", updatable = false, nullable = false)
	Long dealId;

	@Column(name = "booking_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date bookingDate;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "property_type_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	PropertyType propertyType;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "property_name_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	PropertyName propertyName;

	@Column(name = "deal_amount")
	Double dealAmount;

	@Column(name = "loan_required")
	Boolean loanRequired;

	Double loanAmount= Double.valueOf(0);

	Double customerAmount;

	String bankName;

	@Enumerated(EnumType.STRING)
	LoanStatusEnum loanStatus;

	@Enumerated(EnumType.STRING)
	CustomerStatusEnum customerStatus;

	Double supplementAmount=Double.valueOf(0);

	@Column(name = "details", length = 150)
	@Size(max = 150)
	String details;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "lead_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	ClosedLeads lead;

	@NotAudited
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=true and cps.isCustomerPayment=true)")
	Double totalReceivedCustomer;

	@NotAudited
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=false and cps.isCustomerPayment=true)")
	Double totalPendingCustomer;

	@NotAudited
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=false and cps.isCustomerPayment=false)")
	Double totalPendingBank;

	@NotAudited
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=true and cps.isCustomerPayment=false)")
	Double totalReceivedBank;

	@NotAudited
	@Formula("(SELECT ((CASE WHEN deal_amount <> 0 THEN deal_amount ELSE 0 END) + (CASE WHEN supplementAmount <> 0 THEN supplementAmount ELSE 0 END))/10 FROM customer_deal_structure cds WHERE cds.deal_id=deal_id)")
	Double tenPercentOfTotalAmount;

	@NotAudited
	@Formula("(SELECT ((CASE WHEN deal_amount <> 0 THEN deal_amount ELSE 0 END) + (CASE WHEN supplementAmount <> 0 THEN supplementAmount ELSE 0 END))/10 - " +
			"(SELECT CASE WHEN SUM(cpr.amount) IS NULL THEN 0 ELSE SUM(cpr.amount) END FROM customer_payment_received cpr WHERE cpr.deal_id = cds.deal_id) " +
			"FROM customer_deal_structure cds WHERE cds.deal_id=deal_id)")
	Double remainingOfTenPercentTotalAmount;
}
