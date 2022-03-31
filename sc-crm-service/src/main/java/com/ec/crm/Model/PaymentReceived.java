package com.ec.crm.Model;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
	Long scheduleId;

	@Column(name = "payment_date")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date paymentDate;

	@Column(name = "mode")
	String mode;

	@Column(name = "amount")
	Double amount;

	@Column(name = "bank_name", length = 150)
	@Size(max = 150)
	String bankName;

	@Column(name = "cheque_number", length = 150)
	@Size(max = 150)
	String chequeNumber;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "deal_id", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	@NotFound(action = NotFoundAction.IGNORE)
	DealStructure ds;

	@Column(nullable = false)
	Boolean isCustomerPayment;
}
