package com.ec.application.model;

import javax.persistence.CascadeType;
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

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(name = "inward_outward_entries")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class InwardOutwardList extends ReusableFields
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long entryid;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="productId",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	Product product;
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double quantity;
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double closingStock;

	public Long getEntryid() {
		return entryid;
	}


	public void setEntryid(Long entryid) {
		this.entryid = entryid;
	}


	public void setClosingStock(Double closingStock) {
		this.closingStock = closingStock;
	}

	public Double getClosingStock() {
		return closingStock;
	}

	public Product getProduct() {
		return product;
	}
	public void setProduct(Product product) {
		this.product = product;
	}
	public Double getQuantity() {
		return quantity;
	}
	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}
	
	
}
