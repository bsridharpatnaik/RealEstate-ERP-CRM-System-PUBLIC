
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

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
public class DealStructure extends ReusableFields implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "deal_id", updatable = false, nullable = false)
	Long dealId;

	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String phase;

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

	@Column(name = "booking_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date bookingDate;

	@Column(name = "mode")
	String mode;

	@Column(name = "amount")
	Double amount;

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
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=true)")
	Double totalReceived;

	@NotAudited
	@Formula("(select case when sum(cps.amount) is null then 0 else sum(cps.amount) end from  customer_payment_schedule cps where cps.deal_id=deal_id and is_deleted=0 and cps.isReceived=false)")
	Double totalPending;
}
