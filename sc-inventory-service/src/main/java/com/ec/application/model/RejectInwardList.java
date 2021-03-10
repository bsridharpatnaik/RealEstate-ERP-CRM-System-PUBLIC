package com.ec.application.model;

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
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reject_inward_entries")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectInwardList extends ReusableFields
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long rejectentryid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	@Column(nullable = false)
	@NonNull
	Date rejectDate;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "productId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Product product;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double oldQuantity;

	@JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
	Double rejectQuantity;

	Double closingStock;

	String remarks;

	public RejectInwardList(Date returnDate, Product product, Double oldQuantity, Double returnQuantity,
			Double closingStock, String remarks)
	{
		super();
		this.rejectDate = returnDate;
		this.product = product;
		this.oldQuantity = oldQuantity;
		this.rejectQuantity = returnQuantity;
		this.closingStock = closingStock;
		this.remarks = remarks;
	}
}
