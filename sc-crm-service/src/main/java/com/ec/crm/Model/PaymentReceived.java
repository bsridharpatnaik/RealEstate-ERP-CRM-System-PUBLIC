package com.ec.crm.Model;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.crm.Enums.PaymentReceivedFromEnum;
import com.ec.crm.Enums.PaymentReceivedPaymentModeEnum;
import com.ec.crm.ReusableClasses.DynamicAuthorizationEnumJsonSerializer;
import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

@Entity

@Table(name = "customer_payment_received")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited(withModifiedFlag = true)
@Data
public class PaymentReceived extends ReusableFields implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "payment_id", updatable = false, nullable = false)
	Long paymentId;

	@Column(name = "payment_received_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentReceivedDate;

	@Column(name = "payment_by")
	@JsonSerialize(using= DynamicAuthorizationEnumJsonSerializer.class)
	PaymentReceivedFromEnum paymentBy;

	@Column(name = "payment_mode")
	@JsonSerialize(using= DynamicAuthorizationEnumJsonSerializer.class)
	PaymentReceivedPaymentModeEnum paymentMode;

	@Column(name = "amount")
	@JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
	Double amount;

	@Column(name = "bank_name", length = 150)
	@Size(max = 150)
	String bankName;

	@Column(name = "reference_number", length = 150)
	@Size(max = 150)
	String referenceNumber;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "deal_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	DealStructure ds;
}
