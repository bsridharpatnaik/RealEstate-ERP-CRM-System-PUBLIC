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

@Entity
@Table(name = "return_outward_entries")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class ReturnOutwardList extends ReusableFields
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long returnentryid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
	@Column(nullable = false)
	@NonNull
	Date returnDate;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="productId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Product product;
	
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double oldQuantity;
	
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double returnQuantity;
	
	Double closingStock;

	
	public ReturnOutwardList(Date returnDate, Product product, Double oldQuantity, Double newQuantity,
			Double closingStock) {
		super();
		this.returnDate = returnDate;
		this.product = product;
		this.oldQuantity = oldQuantity;
		this.returnQuantity = newQuantity;
		this.closingStock = closingStock;
	}

	
	public ReturnOutwardList() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Long getReturnentryid() {
		return returnentryid;
	}

	public void setReturnentryid(Long returnentryid) {
		this.returnentryid = returnentryid;
	}

	public Date getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(Date returnDate) {
		this.returnDate = returnDate;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Double getOldQuantity() {
		return oldQuantity;
	}

	public void setOldQuantity(Double oldQuantity) {
		this.oldQuantity = oldQuantity;
	}

	public Double getNewQuantity() {
		return returnQuantity;
	}

	public void setNewQuantity(Double newQuantity) {
		this.returnQuantity = newQuantity;
	}

	public Double getClosingStock() {
		return closingStock;
	}

	public void setClosingStock(Double closingStock) {
		this.closingStock = closingStock;
	}
}
